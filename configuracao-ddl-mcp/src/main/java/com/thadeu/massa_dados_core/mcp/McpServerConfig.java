package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Configuração do endpoint MCP para o servidor configuracao-ddl-mcp.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Expor o endpoint {@code POST /mcp} para comunicação JSON-RPC</li>
 *   <li>Expor o endpoint {@code GET /mcp} para listar ferramentas (inicialização do cliente)</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@RestController
@RequestMapping("/mcp")
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    private final McpToolHandler handler;
    private final ObjectMapper objectMapper;

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
     * Endpoint MCP que recebe requisições JSON-RPC via POST.
     *
     * @param bodyString corpo da requisição como string JSON
     * @return resposta HTTP com resultado ou erro JSON-RPC
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcp(@RequestBody String bodyString) {
        log.info("[handleMcp] Requisição MCP recebida em /mcp");
        log.debug("[handleMcp] Corpo recebido: {}", bodyString);

        try {
            JsonNode body = objectMapper.readTree(bodyString);
            String method = body.has("method") ? body.get("method").asText() : "";
            JsonNode params = body.has("params") ? body.get("params") : objectMapper.nullNode();
            JsonNode id = body.has("id") ? body.get("id") : null;

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

    /**
     * Endpoint GET para listar ferramentas (usado pelo cliente MCP durante inicialização).
     *
     * @return resposta HTTP com lista de ferramentas no formato JSON-RPC
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> handleMcpGet() {
        log.info("[handleMcpGet] Requisição GET recebida em /mcp - retornando lista de ferramentas");
        return handler.handleToolsList(null);
    }
}
