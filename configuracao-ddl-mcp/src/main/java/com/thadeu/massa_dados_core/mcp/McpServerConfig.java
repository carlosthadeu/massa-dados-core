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

import java.io.IOException;
import java.io.OutputStream;
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
 *   <li>Expor o endpoint {@code GET /mcp} para estabelecer canal SSE</li>
 *   <li>Expor o endpoint {@code POST /mcp} para receber requisições JSON-RPC</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 14.0
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    // Armazena o OutputStream de cada sessão ativa para comunicação bidirecional
    private final ConcurrentHashMap<String, OutputStream> activeStreams = new ConcurrentHashMap<>();

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
     * Endpoint GET que estabelece o canal SSE.
     *
     * <p>Escreve o handshake diretamente no OutputStream do HttpServletResponse
     * antes de retornar, garantindo que o cliente receba o evento endpoint imediatamente.</p>
     *
     * @param response objeto HttpServletResponse para escrever o stream SSE
     * @return {@link DeferredResult} que mantém a conexão aberta
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public DeferredResult<Void> connect(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();

        // Configura headers SSE manualmente
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        log.info("[connect] Nova sessão SSE registrada: {}", sessionId);

        // Monta o endpoint que o ChatMCP usará para enviar os comandos POST
        String messageEndpointUrl = "http://localhost:8081/mcp?sessionId=" + sessionId;

        // Escreve o handshake diretamente no OutputStream antes de retornar
        try {
            OutputStream outputStream = response.getOutputStream();
            activeStreams.put(sessionId, outputStream);
            log.info("[connect] OutputStream registrado para sessão {}", sessionId);

            // Envia o handshake inicial (evento "endpoint") diretamente no OutputStream
            String handshakeEvent = "event: endpoint\ndata: " + messageEndpointUrl + "\n\n";
            synchronized (outputStream) {
                outputStream.write(handshakeEvent.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
            log.info("[connect] Handshake inicial 'endpoint' enviado com sucesso para sessão {}", sessionId);
        } catch (IOException e) {
            log.error("[connect] Erro ao enviar handshake para sessão {}", sessionId, e);
            activeStreams.remove(sessionId);
            DeferredResult<Void> errorResult = new DeferredResult<>();
            errorResult.setErrorResult(e);
            return errorResult;
        }

        // Cria DeferredResult com timeout de 30 minutos
        DeferredResult<Void> deferredResult = new DeferredResult<>(1_800_000L);

        // Agenda heartbeats a cada 15 segundos para manter a conexão viva
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            OutputStream os = activeStreams.get(sessionId);
            if (os == null) {
                return;
            }
            try {
                synchronized (os) {
                    // Comentário SSE vazio como keep-alive (formato: ": keep-alive\n\n")
                    os.write(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (IOException e) {
                log.info("[connect] Conexão encerrada pelo cliente para a sessão: {}", sessionId);
                activeStreams.remove(sessionId);
                deferredResult.setResult(null);
            }
        }, 15, 15, TimeUnit.SECONDS);

        // Callback quando a conexão é finalizada (cliente desconecta ou timeout)
        deferredResult.onCompletion(() -> {
            log.info("[connect] Sessão SSE finalizada: {}", sessionId);
            activeStreams.remove(sessionId);
        });

        return deferredResult;
    }

    /**
     * Endpoint POST que recebe comandos JSON-RPC e responde no respectivo OutputStream ativo.
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

        OutputStream outputStream = activeStreams.get(sessionId);
        if (outputStream == null) {
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

                // Envia o evento "message" diretamente no OutputStream
                String messageEvent = "event: message\ndata: " + responseJson + "\n\n";
                synchronized (outputStream) {
                    outputStream.write(messageEvent.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
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
                    synchronized (outputStream) {
                        outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }
            } catch (IOException ex) {
                log.error("[handlePost] Erro ao enviar resposta de erro via SSE", ex);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
