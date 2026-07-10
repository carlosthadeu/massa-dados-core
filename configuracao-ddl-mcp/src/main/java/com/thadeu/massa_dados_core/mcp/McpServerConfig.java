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
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Configuração do endpoint MCP para o cliente ChatMCP (Dart/EventFlux).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Expor o endpoint {@code GET /mcp} para estabelecer canal SSE via {@link ResponseBodyEmitter}</li>
 *   <li>Expor o endpoint {@code POST /mcp} para receber requisições JSON-RPC</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 12.0
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    // Armazena o ResponseBodyEmitter de cada sessão ativa para comunicação bidirecional
    private final ConcurrentHashMap<String, ResponseBodyEmitter> activeEmitters = new ConcurrentHashMap<>();

    // Executor para envio de heartbeats sem bloquear threads do container
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2);

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
     * Endpoint GET que estabelece o canal SSE usando {@link ResponseBodyEmitter}.
     *
     * <p>O Spring gerencia corretamente a resposta assíncrona, liberando a thread
     * do container enquanto mantém a conexão aberta.</p>
     *
     * @param response objeto HttpServletResponse para configurar headers manualmente
     * @return {@link DeferredResult} que contém o {@link ResponseBodyEmitter}
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public DeferredResult<ResponseBodyEmitter> connect(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();

        // Configura headers SSE manualmente antes de retornar o DeferredResult
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        log.info("[connect] Nova sessão SSE registrada: {}", sessionId);

        // Monta o endpoint que o ChatMCP usará para enviar os comandos POST
        String messageEndpointUrl = "http://localhost:8081/mcp?sessionId=" + sessionId;

        // Cria o DeferredResult com timeout de 30 minutos
        DeferredResult<ResponseBodyEmitter> deferredResult = new DeferredResult<>(1_800_000L);

        // Cria o ResponseBodyEmitter
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(1_800_000L);

        activeEmitters.put(sessionId, emitter);
        log.info("[connect] ResponseBodyEmitter registrado para sessão {}", sessionId);

        // Envia o handshake inicial (evento "endpoint") diretamente no ResponseBodyEmitter
        try {
            String handshakeEvent = "event: endpoint\ndata: " + messageEndpointUrl + "\n\n";
            emitter.send(handshakeEvent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);
            log.info("[connect] Handshake inicial 'endpoint' enviado com sucesso para sessão {}", sessionId);
        } catch (IOException e) {
            log.error("[connect] Erro ao enviar handshake para sessão {}", sessionId, e);
            activeEmitters.remove(sessionId);
            deferredResult.setErrorResult(e);
            return deferredResult;
        }

        // Agenda heartbeats a cada 15 segundos para manter a conexão viva
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            ResponseBodyEmitter em = activeEmitters.get(sessionId);
            if (em == null) {
                return;
            }
            try {
                // Comentário SSE vazio como keep-alive (formato: ": keep-alive\n\n")
                em.send(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);
            } catch (IOException e) {
                log.info("[connect] Conexão encerrada pelo cliente para a sessão: {}", sessionId);
                activeEmitters.remove(sessionId);
                try {
                    em.complete();
                } catch (Exception ignored) {
                }
            }
        }, 15, 15, TimeUnit.SECONDS);

        // Callback quando a conexão é finalizada (cliente desconecta ou timeout)
        emitter.onCompletion(() -> {
            log.info("[connect] Sessão SSE finalizada: {}", sessionId);
            activeEmitters.remove(sessionId);
        });

        emitter.onTimeout(() -> {
            log.info("[connect] Sessão SSE expirou por timeout: {}", sessionId);
            activeEmitters.remove(sessionId);
        });

        emitter.onError(ex -> {
            log.warn("[connect] Erro na sessão SSE {}: {}", sessionId, ex.getMessage());
            activeEmitters.remove(sessionId);
        });

        // Define o emitter como resultado do DeferredResult
        deferredResult.setResult(emitter);

        return deferredResult;
    }

    /**
     * Endpoint POST que recebe comandos JSON-RPC e responde no respectivo ResponseBodyEmitter ativo.
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

        ResponseBodyEmitter emitter = activeEmitters.get(sessionId);
        if (emitter == null) {
            log.warn("[handlePost] Nenhuma conexão ativa encontrada para a sessão: {}", sessionId);
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
                    log.warn("[handlePost] Método desconhecido: {}", method);
                    yield handler.errorResponse(-32601, "Method not found: " + method, id);
                }
            };

            if (response.getBody() != null) {
                String responseJson = objectMapper.writeValueAsString(response.getBody());

                // Envia o evento "message" diretamente no ResponseBodyEmitter
                String messageEvent = "event: message\ndata: " + responseJson + "\n\n";
                emitter.send(messageEvent.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN);
                log.info("[handlePost] Resposta para o método '{}' enviada via SSE", method);
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
