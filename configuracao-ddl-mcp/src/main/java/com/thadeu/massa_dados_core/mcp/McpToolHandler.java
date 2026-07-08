package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

@Component
public class McpToolHandler {

    private final ObjectMapper objectMapper;
    private final McpDdlToEntityService ddlToEntityService;
    private final McpUnknownEntityService unknownEntityService;

    public McpToolHandler(ObjectMapper objectMapper,
                          McpDdlToEntityService ddlToEntityService,
                          McpUnknownEntityService unknownEntityService) {
        this.objectMapper = objectMapper;
        this.ddlToEntityService = ddlToEntityService;
        this.unknownEntityService = unknownEntityService;
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
            case "ddl_to_entity" -> handleDdlToEntity(arguments, id);
            case "identify_unknown_entities" -> handleIdentifyUnknownEntities(arguments, id);
            default -> errorResponse(-32602, "Unknown tool: " + toolName, id);
        };
    }

    private ServerResponse handleDdlToEntity(JsonNode arguments, JsonNode id) {
        try {
            DdlRequest ddlRequest = objectMapper.treeToValue(arguments, DdlRequest.class);
            DdlResponse response = ddlToEntityService.processDdl(ddlRequest);
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error processing DDL: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleIdentifyUnknownEntities(JsonNode arguments, JsonNode id) {
        try {
            UnknownEntityRequest request = objectMapper.treeToValue(arguments, UnknownEntityRequest.class);
            UnknownEntityResponse response = unknownEntityService.identify(request);
            return successResponse(response, id);
        } catch (Exception e) {
            return errorResponse(-32603, "Error identifying unknown entities: " + e.getMessage(), id);
        }
    }

    private ServerResponse handleToolsList(JsonNode id) {
        var tools = java.util.List.of(
                Map.of(
                        "name", "ddl_to_entity",
                        "description", "Converte um script DDL em uma classe Entity JPA e salva no projeto do Servidor 2",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "ddlScript", Map.of(
                                                "type", "string",
                                                "description", "Script DDL (CREATE TABLE, ALTER TABLE, etc.)"
                                        )
                                ),
                                "required", java.util.List.of("ddlScript")
                        )
                ),
                Map.of(
                        "name", "identify_unknown_entities",
                        "description", "Compara tabelas/colunas do DDL com as classes Entity existentes e identifica itens não reconhecidos",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "ddlScript", Map.of(
                                                "type", "string",
                                                "description", "Script DDL para análise"
                                        )
                                ),
                                "required", java.util.List.of("ddlScript")
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
