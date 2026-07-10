package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
 * @version 3.0
 */
@RestController
@RequestMapping("/mcp")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    /**
     * Mapa de emissores SSE, um por sessão (identificado pelo sessionId).
     */
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

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
     * Endpoint GET que estabelece um canal SSE (Server-Sent Events).
     *
     * <p>O primeiro evento enviado contém a URL para onde o cliente deve
     * enviar os POSTs subsequentes (endpoint de mensagens).</p>
     *
     * @return emissor SSE
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        log.info("[connect] Nova conexão SSE estabelecida");

        // Timeout de 30 minutos para evitar desconexões prematuras
        SseEmitter emitter = new SseEmitter(1800000L);
        String sessionId = UUID.randomUUID().toString();

        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> {
            log.info("[connect] Conexão SSE concluída para sessão {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("[connect] Conexão SSE expirou para sessão {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onError(ex -> {
            log.error("[connect] Erro na conexão SSE para sessão {}", sessionId, ex);
            emitters.remove(sessionId);
        });

        // Envia o handshake inicial imediatamente para o ChatMCP registrar o endpoint
        try {
            // O evento DEVE se chamar "endpoint" e o dado deve ser a URL para POST
            String messageEndpointUrl = "http://localhost:8081/mcp?sessionId=" + sessionId;

            // Envia manualmente no formato SSE esperado pelo cliente Dart
            // O formato correto é:
            // event: endpoint
            // data: http://localhost:8081/mcp?sessionId=xxx
            // 
            emitter.send("event: endpoint\ndata: " + messageEndpointUrl + "\n\n");

            log.info("[connect] Evento 'endpoint' enviado para sessão {}: {}", sessionId, messageEndpointUrl);
        } catch (IOException e) {
            log.error("[connect] Erro ao enviar evento 'endpoint' para sessão {}", sessionId, e);
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

        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            log.warn("[handlePost] Sessão não encontrada: {}", sessionId);
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

            // Enviar resposta via SSE no evento "message"
            if (response.getBody() != null) {
                String responseJson = objectMapper.writeValueAsString(response.getBody());
                // Envia manualmente no formato SSE esperado pelo cliente Dart
                emitter.send("event: message\ndata: " + responseJson + "\n\n");
                log.debug("[handlePost] Resposta enviada via SSE para sessão {}", sessionId);
            }

            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("[handlePost] Erro interno ao processar requisição", e);
            try {
                Map<String, Object> errorBody = handler.errorResponse(-32603, "Internal error: " + e.getMessage(), null).getBody();
                if (errorBody != null) {
                    String errorJson = objectMapper.writeValueAsString(errorBody);
                    emitter.send("event: message\ndata: " + errorJson + "\n\n");
                }
            } catch (IOException ex) {
                log.error("[handlePost] Erro ao enviar resposta de erro via SSE", ex);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
