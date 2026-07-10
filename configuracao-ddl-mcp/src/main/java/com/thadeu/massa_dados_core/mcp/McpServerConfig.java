package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuração do endpoint MCP para o servidor configuracao-ddl-mcp usando ResponseBodyEmitter.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Expor o endpoint {@code GET /mcp} para estabelecer canal SSE</li>
 *   <li>Expor o endpoint {@code POST /mcp} para receber requisições JSON-RPC</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 6.0
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    // Usando ResponseBodyEmitter para controle total dos bytes enviados
    private final ConcurrentHashMap<String, ResponseBodyEmitter> emitters = new ConcurrentHashMap<>();

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
     * Endpoint GET que estabelece a conexão SSE usando ResponseBodyEmitter bruto.
     *
     * <p>Configura manualmente os headers HTTP antes de qualquer validação interna do Spring
     * e envia os bytes brutos do handshake SSE.</p>
     *
     * @param response objeto HttpServletResponse para forçar cabeçalhos SSE manualmente
     * @return emissor de corpo de resposta
     */
    @GetMapping
    public ResponseBodyEmitter connect(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();
        log.info("[connect] Nova conexão SSE estabelecida para sessão {}", sessionId);

        // Configura manualmente os headers HTTP antes de qualquer validação interna do Spring
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        // Mantém aberto por 30 minutos
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(1800000L);
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> {
            log.info("[connect] Conexão SSE concluída para sessão {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.warn("[connect] Conexão SSE expirou para sessão {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onError((ex) -> {
            log.error("[connect] Erro na conexão SSE para sessão {}", sessionId, ex);
            emitters.remove(sessionId);
        });

        try {
            String messageEndpointUrl = "http://localhost:8081/mcp?sessionId=" + sessionId;

            // Constrói o texto bruto exatamente no padrão do protocolo SSE
            String handshakeEvent = "event: endpoint\ndata: " + messageEndpointUrl + "\n\n";

            // Envia como bytes puros, contornando conversores de objetos (Jackson/MediaTypes)
            emitter.send(handshakeEvent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);

            log.info("[connect] Conexão SSE estabelecida com sucesso. Sessão: {}", sessionId);
        } catch (IOException e) {
            log.error("[connect] Falha ao enviar handshake inicial para sessão {}", sessionId, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Endpoint POST que recebe os comandos do ChatMCP.
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

        log.info("[handlePost] Requisição recebida para sessão {}", sessionId);

        ResponseBodyEmitter emitter = emitters.get(sessionId);
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
                    log.warn("[handlePost] Método não suportado: {}", method);
                    yield handler.errorResponse(-32601, "Method not found: " + method, id);
                }
            };

            if (response.getBody() != null) {
                String responseJson = objectMapper.writeValueAsString(response.getBody());

                // Constrói a mensagem JSON-RPC encapsulada no evento "message" exigido pelo MCP
                String messageEvent = "event: message\ndata: " + responseJson + "\n\n";

                emitter.send(messageEvent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);
                log.debug("[handlePost] Resposta para o método {} enviada via Stream", method);
            }

            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("[handlePost] Erro ao processar payload JSON-RPC", e);
            try {
                Map<String, Object> errorBody = handler.errorResponse(-32603, "Internal error: " + e.getMessage(), null).getBody();
                if (errorBody != null) {
                    String errorJson = objectMapper.writeValueAsString(errorBody);
                    String errorEvent = "event: message\ndata: " + errorJson + "\n\n";
                    emitter.send(errorEvent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);
                }
            } catch (IOException ex) {
                log.error("[handlePost] Erro ao enviar resposta de erro via SSE", ex);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
