package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataRequest;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

@Component
public class McpToolHandler {

    private final ObjectMapper objectMapper;
    private final McpEntityMetadataService metadataService;
    private final McpMapeamentoSemanticoService mapeamentoSemanticoService;

    public McpToolHandler(ObjectMapper objectMapper,
                          McpEntityMetadataService metadataService,
                          McpMapeamentoSemanticoService mapeamentoSemanticoService) {
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
        this.mapeamentoSemanticoService = mapeamentoSemanticoService;
    }

    public ServerResponse handle(ServerRequest request) {
        try {
            JsonNode body = objectMapper.readTree(request.servletRequest().getInputStream());
            String method = body.has("method") ? body.get("method").asText() : "";
            JsonNode params = body.has("params") ? body.get("params") : objectMapper.nullNode();
            JsonNode id = body.has("id") ? body.get("id") : objectMapper.nullNode();

            return switch (method) {
                case "tools/call" -> handleToolCall(params, id);
                case "tools/list" -> handleToolsList(id);
                default -> errorResponse(-32601, "Method not found: " + method, id);
            };
        } catch (Exception e) {
            return errorResponse(-32603, "Internal error: " + e.getMessage(), null);
        }
    }

    private ServerResponse handleToolCall(JsonNode params, JsonNode id) {
        String toolName = params.has("name") ? params.get("name").asText() : "";
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.nullNode();

        return switch (toolName) {
            case "get_entity_metadata" -> handleGetEntityMetadata(arguments, id);
            case "get_mapeamento_semantico" -> handleGetMapeamentoSemantico(arguments, id);
            default -> errorResponse(-32602, "Unknown tool: " + toolName, id);
        };
    }

    private ServerResponse handleGetEntityMetadata(JsonNode arguments, JsonNode id) {
        try {
            EntityMetadataRequest request = objectMapper.treeToValue(arguments, EntityMetadataRequest.class);
            EntityMetadataResponse response = metadataService.getMetadata(request);
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error getting metadata: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleGetMapeamentoSemantico(JsonNode arguments, JsonNode id) {
        try {
            var response = Map.of(
                    "mapeamentoSemantico", mapeamentoSemanticoService.getMapeamentoSemanticoJson(),
                    "sinonimos", mapeamentoSemanticoService.getSinonimosJson()
            );
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error getting mapeamento semantico: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleToolsList(JsonNode id) {
        var tools = java.util.List.of(
                Map.of(
                        "name", "get_entity_metadata",
                        "description", "Retorna metadados de uma classe Entity JPA (nome da tabela, atributos, relacionamentos)",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "className", Map.of(
                                                "type", "string",
                                                "description", "Nome completo da classe (ex: br.gov.bnb.domain.entity.Portfolio)"
                                        )
                                ),
                                "required", java.util.List.of("className")
                        )
                ),
                Map.of(
                        "name", "get_mapeamento_semantico",
                        "description", "Retorna o mapeamento semântico completo (entidades, atributos, valores de enum, sinônimos) para interpretação de comandos em linguagem natural",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(),
                                "required", java.util.List.of()
                        )
                )
        );

        var result = Map.of("tools", tools);
        return successResponse(result, id);
    }

    private ServerResponse successResponse(Object result, JsonNode id) {
        var body = Map.of(
                "jsonrpc", "2.0",
                "result", Map.of("content", java.util.List.of(Map.of("type", "text", "text", toJsonString(result)))),
                "id", id
        );
        return ServerResponse.ok().body(body);
    }

    private ServerResponse errorResponse(int code, String message, JsonNode id) {
        var body = Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", code, "message", message),
                "id", id
        );
        return ServerResponse.status(400).body(body);
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
