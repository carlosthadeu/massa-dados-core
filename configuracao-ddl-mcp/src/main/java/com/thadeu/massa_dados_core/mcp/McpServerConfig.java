package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
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
 * @version 2.0
 */
@RestController
@RequestMapping("/mcp")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    /**
     * Mapa de sinks SSE, um por sessão (identificado pelo sessionId).
     */
    private final ConcurrentHashMap<String, Sinks.Many<String>> sseSinks = new ConcurrentHashMap<>();

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
     * @return fluxo de eventos SSE
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<String>> handleSse() {
        log.info("[handleSse] Nova conexão SSE estabelecida");

        // Gerar um identificador único para esta sessão
        String sessionId = java.util.UUID.randomUUID().toString();

        // Criar um sink para esta sessão
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        sseSinks.put(sessionId, sink);

        // URL para onde o cliente deve enviar os POSTs
        String messageUrl = "/mcp/message/" + sessionId;

        // Primeiro evento: endpoint de mensagens
        sink.tryEmitNext("event: endpoint\ndata: " + messageUrl + "\n\n");

        // Evento de keep-alive a cada 30 segundos
        Flux<Long> keepAlive = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> {
                    sink.tryEmitNext(": keepalive\n\n");
                    return tick;
                });

        // Fluxo principal: eventos do sink
        Flux<org.springframework.http.codec.ServerSentEvent<String>> eventFlux = sink.asFlux()
                .map(data -> org.springframework.http.codec.ServerSentEvent.<String>builder()
                        .data(data)
                        .build())
                .doOnCancel(() -> {
                    log.info("[handleSse] Conexão SSE cancelada para sessão {}", sessionId);
                    sseSinks.remove(sessionId);
                })
                .doOnTerminate(() -> {
                    log.info("[handleSse] Conexão SSE encerrada para sessão {}", sessionId);
                    sseSinks.remove(sessionId);
                });

        // Combinar keep-alive com eventos
        return Flux.merge(eventFlux, keepAlive.flatMap(tick -> Flux.empty()));
    }

    /**
     * Endpoint GET alternativo para clientes que não suportam SSE.
     * Retorna a lista de ferramentas disponíveis (compatibilidade).
     *
     * @return resposta HTTP com lista de ferramentas no formato JSON-RPC
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> handleMcpGet() {
        log.info("[handleMcpGet] Requisição GET recebida em /mcp - retornando lista de ferramentas");
        return handler.handleToolsList(null);
    }

    /**
     * Endpoint POST para receber mensagens JSON-RPC de uma sessão SSE específica.
     *
     * @param sessionId identificador da sessão SSE
     * @param body      corpo da requisição JSON-RPC
     * @return resposta HTTP com resultado ou erro JSON-RPC
     */
    @PostMapping("/message/{sessionId}")
    public ResponseEntity<Map<String, Object>> handleMessage(
            @PathVariable String sessionId,
            @RequestBody String body) {
        log.info("[handleMessage] Mensagem recebida para sessão {}", sessionId);

        if (body == null || body.trim().isEmpty()) {
            log.warn("[handleMessage] Corpo da requisição vazio");
            return handler.errorResponse(-32602, "Corpo da requisição não pode ser vazio", null);
        }

        try {
            JsonNode jsonBody = objectMapper.readTree(body);
            String method = jsonBody.has("method") ? jsonBody.get("method").asText() : "";
            JsonNode params = jsonBody.has("params") ? jsonBody.get("params") : objectMapper.nullNode();
            JsonNode id = jsonBody.has("id") ? jsonBody.get("id") : null;

            log.debug("[handleMessage] Método: {}, id: {}", method, id);

            ResponseEntity<Map<String, Object>> response = switch (method) {
                case "tools/call" -> handler.handleToolCall(params, id);
                case "tools/list" -> handler.handleToolsList(id);
                default -> {
                    log.warn("[handleMessage] Método não encontrado: {}", method);
                    yield handler.errorResponse(-32601, "Method not found: " + method, id);
                }
            };

            // Enviar resposta via SSE se houver sink para esta sessão
            Sinks.Many<String> sink = sseSinks.get(sessionId);
            if (sink != null && response.getBody() != null) {
                String responseJson = objectMapper.writeValueAsString(response.getBody());
                sink.tryEmitNext("event: message\ndata: " + responseJson + "\n\n");
            }

            return response;
        } catch (Exception e) {
            log.error("[handleMessage] Erro interno ao processar requisição", e);
            return handler.errorResponse(-32603, "Internal error: " + e.getMessage(), null);
        }
    }

    /**
     * Endpoint POST legado para compatibilidade com clientes que não usam SSE.
     *
     * @param body corpo da requisição JSON-RPC
     * @return resposta HTTP com resultado ou erro JSON-RPC
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcp(@RequestBody String body) {
        log.info("[handleMcp] Requisição MCP recebida em /mcp (legado)");

        if (body == null || body.trim().isEmpty()) {
            log.warn("[handleMcp] Corpo da requisição vazio");
            return handler.errorResponse(-32602, "Corpo da requisição não pode ser vazio", null);
        }

        try {
            JsonNode jsonBody = objectMapper.readTree(body);
            String method = jsonBody.has("method") ? jsonBody.get("method").asText() : "";
            JsonNode params = jsonBody.has("params") ? jsonBody.get("params") : objectMapper.nullNode();
            JsonNode id = jsonBody.has("id") ? jsonBody.get("id") : null;

            log.debug("[handleMcp] Método: {}, id: {}", method, id);

            return switch (method) {
                case "tools/call" -> handler.handleToolCall(params, id);
                case "tools/list" -> handler.handleToolsList(id);
                default -> {
                    log.warn("[handleMcp] Método não encontrado: {}", method);
                    yield handler.errorResponse(-32601, "Method not found: " + method, id);
                }
            };
        } catch (Exception e) {
            log.error("[handleMcp] Erro interno ao processar requisição", e);
            return handler.errorResponse(-32603, "Internal error: " + e.getMessage(), null);
        }
    }
}
