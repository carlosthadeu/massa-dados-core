package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuração estável do endpoint MCP para o cliente ChatMCP (Dart/EventFlux).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Expor o endpoint {@code GET /mcp} para estabelecer canal SSE</li>
 *   <li>Expor o endpoint {@code POST /mcp} para receber requisições JSON-RPC</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 7.0
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

    // Armazena o OutputStream bruto de cada sessão ativa para comunicação bidirecional direta
    private final ConcurrentHashMap<String, OutputStream> activeStreams = new ConcurrentHashMap<>();

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
     * Endpoint GET que estabelece o canal SSE escrevendo diretamente no OutputStream nativo.
     * Retorna 'void' para impedir que o Spring MVC interfile ou modifique a resposta.
     *
     * @param response objeto HttpServletResponse para escrever o stream SSE
     */
    @GetMapping
    public void connect(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();

        // Configura rigorosamente os headers HTTP puros que o EventFlux exige
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            OutputStream outputStream = response.getOutputStream();
            activeStreams.put(sessionId, outputStream);

            log.info("[connect] Nova sessão SSE registrada: {}", sessionId);

            // Monta o endpoint que o ChatMCP usará para enviar os comandos POST
            String messageEndpointUrl = "http://localhost:8081/mcp?sessionId=" + sessionId;

            // Formatação estrita exigida pelo parser do EventFlux (Dart)
            String handshakeEvent = "event: endpoint\ndata: " + messageEndpointUrl + "\n\n";

            // Escreve os bytes puros no socket e limpa o buffer imediatamente
            outputStream.write(handshakeEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            log.info("[connect] Handshake inicial 'endpoint' enviado com sucesso para sessão {}", sessionId);

            // Mantém a thread síncrona viva simulando o canal aberto sem delegar para o Spring
            while (activeStreams.containsKey(sessionId)) {
                try {
                    Thread.sleep(15000); // Heartbeat/Keep-alive a cada 15 segundos

                    // Envia um comentário SSE vazio como keep-alive para evitar queda por timeout do Gateway/Nginx
                    synchronized (outputStream) {
                        outputStream.write(":\n\n".getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    log.info("[connect] Conexão encerrada pelo cliente para a sessão: {}", sessionId);
                    break;
                }
            }
        } catch (IOException e) {
            log.error("[connect] Erro crítico no stream da sessão {}", sessionId, e);
        } finally {
            activeStreams.remove(sessionId);
            log.info("[connect] Sessão SSE finalizada e removida: {}", sessionId);
        }
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

                // Formata o evento exatamente conforme a especificação do protocolo MCP
                String messageEvent = "event: message\ndata: " + responseJson + "\n\n";

                synchronized (outputStream) {
                    outputStream.write(messageEvent.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
                log.info("[handlePost] Resposta para o método '{}' enviada via socket", method);
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
