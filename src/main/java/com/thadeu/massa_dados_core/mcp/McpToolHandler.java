package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Manipulador das ferramentas MCP expostas pelo servidor.
 *
 * <p>Registra as ferramentas {@code ddl_to_entity} e {@code identify_unknown_entities}.
 */
@Component
public class McpToolHandler {

    private final McpDdlToEntityService ddlToEntityService;
    private final McpUnknownEntityService unknownEntityService;

    public McpToolHandler(McpDdlToEntityService ddlToEntityService,
                          McpUnknownEntityService unknownEntityService) {
        this.ddlToEntityService = ddlToEntityService;
        this.unknownEntityService = unknownEntityService;
    }

    /**
     * Retorna a lista de ferramentas registradas.
     *
     * @return Lista de {@link McpSchema.Tool}
     */
    public List<McpSchema.Tool> getTools() {
        return List.of(
                new McpSchema.Tool(
                        "ddl_to_entity",
                        "Converte um script DDL em uma classe Entity JPA e salva no projeto",
                        new McpSchema.JsonSchema(
                                "object",
                                java.util.Map.of(
                                        "ddlScript", new McpSchema.JsonSchema(
                                                "string",
                                                "Script DDL contendo CREATE TABLE, ALTER TABLE, etc."
                                        )
                                ),
                                List.of("ddlScript")
                        )
                ),
                new McpSchema.Tool(
                        "identify_unknown_entities",
                        "Identifica tabelas/colunas no DDL que não possuem classe Entity correspondente",
                        new McpSchema.JsonSchema(
                                "object",
                                java.util.Map.of(
                                        "ddlScript", new McpSchema.JsonSchema(
                                                "string",
                                                "Script DDL contendo CREATE TABLE, ALTER TABLE, etc."
                                        )
                                ),
                                List.of("ddlScript")
                        )
                )
        );
    }

    /**
     * Executa a ferramenta solicitada.
     *
     * @param toolName Nome da ferramenta
     * @param arguments Argumentos da ferramenta
     * @return Resultado da execução
     */
    public Object executeTool(String toolName, java.util.Map<String, Object> arguments) {
        return switch (toolName) {
            case "ddl_to_entity" -> {
                String ddlScript = (String) arguments.get("ddlScript");
                List<DdlResponse> responses = ddlToEntityService.convertDdlToEntity(
                        new DdlRequest(ddlScript));
                yield responses;
            }
            case "identify_unknown_entities" -> {
                String ddlScript = (String) arguments.get("ddlScript");
                UnknownEntityResponse response = unknownEntityService.identifyUnknownEntities(
                        new UnknownEntityRequest(ddlScript));
                yield response;
            }
            default -> throw new IllegalArgumentException("Ferramenta desconhecida: " + toolName);
        };
    }
}
