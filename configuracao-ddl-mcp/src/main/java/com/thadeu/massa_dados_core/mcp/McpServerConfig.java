package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuração do endpoint MCP para o servidor configuracao-ddl-mcp.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Expor o endpoint {@code GET /mcp} para estabelecer canal SSE</li>
 *   <li>Expor o endpoint {@code POST /mcp} para receber requisições JSON-RPC</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 5.0
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    /**
     * Mapa de emissores SseEmitter ativos por sessionId.
     */
    private final ConcurrentHashMap<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    /**
     * Construtor com injeção de dependências.
     *
     * @param handler      manipulador de requisições MCP
     * @param objectMapper serializador JSON
     */
    public McpServerConfig(McpToolHandler handler, ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    /**
     * Endpoint GET que estabelece um canal SSE estável usando SseEmitter.
     *
     * <p>O primeiro evento enviado contém a URL para onde o cliente deve
     * enviar os POSTs subsequentes (endpoint de mensagens).</p>
     *
     * @return emissor SSE
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        String sessionId = UUID.randomUUID().toString();
        log.info("[connect] Nova conexão SSE estabelecida para sessão {}", sessionId);

        // Timeout de 30 minutos (1800000ms) para evitar que a IA desconecte no meio de uma análise longa
        SseEmitter emitter = new SseEmitter(1800000L);

        sseEmitters.put(sessionId, emitter);

        // Callbacks para limpar o mapa quando a conexão cair ou expirar
        emitter.onCompletion(() -> {
            log.info("[connect] Conexão SSE concluída para sessão {}", sessionId);
            sseEmitters.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("[connect] Conexão SSE expirou para sessão {}", sessionId);
            sseEmitters.remove(sessionId);
        });
        emitter.onError(ex -> {
            log.error("[connect] Erro na conexão SSE para sessão {}", sessionId, ex);
            sseEmitters.remove(sessionId);
        });

        try {
            // Envia o handshake inicial exigido pelo protocolo MCP
            String messageEndpointUrl = "http://localhost:8081/mcp?sessionId=" + sessionId;

            emitter.send(SseEmitter.event()
                    .name("endpoint") // O ChatMCP busca estritamente por este nome de evento
                    .data(messageEndpointUrl));

            log.info("[connect] Evento 'endpoint' enviado para sessão {}: {}", sessionId, messageEndpointUrl);
        } catch (IOException e) {
            log.warn("[connect] Erro ao enviar handshake inicial para sessão {}", sessionId, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Endpoint POST que recebe comandos JSON-RPC do ChatMCP.
     *
     * <p>O ChatMCP envia os comandos via POST com o parâmetro {@code sessionId}
     * na query string. A resposta é enviada de volta pelo canal SSE.</p>
     *
     * @param sessionId identificador da sessão SSE
     * @param body      corpo da requisição JSON-RPC
     * @return resposta HTTP 202 Accepted
     */
    @PostMapping
    public ResponseEntity<Void> handlePost(
            @RequestParam("sessionId") String sessionId,
            @RequestBody String body) {

        log.info("[handlePost] Mensagem recebida para sessão {}", sessionId);

        // Busca o SseEmitter correto para responder de volta na mesma sessão
        SseEmitter emitter = sseEmitters.get(sessionId);
        if (emitter == null) {
            log.warn("[handlePost] Sessão não encontrada ou expirada: {}", sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (body == null || body.trim().isEmpty()) {
            log.warn("[handlePost] Corpo da requisição vazio");
            return ResponseEntity.badRequest().build();
        }

        try {
            JsonNode jsonBody = objectMapper.readTree(body);
            String method = jsonBody.has("method") ? jsonBody.get("method").asText() : "";
            JsonNode params = jsonBody.has("params") ? jsonBody.get("params") : objectMapper.nullNode();
            JsonNode id = jsonBody.has("id") ? jsonBody.get("id") : null;

            log.debug("[handlePost] Método: {}, id: {}", method, id);

            ResponseEntity<Map<String, Object>> response = switch (method) {
                case "tools/call" -> handler.handleToolCall(params, id);
                case "tools/list" -> handler.handleToolsList(id);
                default -> {
                    log.warn("[handlePost] Método não encontrado: {}", method);
                    yield handler.errorResponse(-32601, "Method not found: " + method, id);
                }
            };

            // Envia a resposta de volta ao canal de Stream aberto no evento "message"
            if (response.getBody() != null) {
                String responseJson = objectMapper.writeValueAsString(response.getBody());

                emitter.send(SseEmitter.event()
                        .name("message") // Exigido pelo protocolo MCP para respostas JSON-RPC
                        .data(responseJson));

                log.debug("[handlePost] Resposta do método {} enviada via SSE para sessão {}", method, sessionId);
            }

            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("[handlePost] Erro interno ao processar requisição", e);
            try {
                Map<String, Object> errorBody = handler.errorResponse(-32603, "Internal error: " + e.getMessage(), null).getBody();
                if (errorBody != null) {
                    String errorJson = objectMapper.writeValueAsString(errorBody);
                    emitter.send(SseEmitter.event().name("message").data(errorJson));
                }
            } catch (IOException ex) {
                log.error("[handlePost] Erro ao enviar resposta de erro via SSE", ex);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
