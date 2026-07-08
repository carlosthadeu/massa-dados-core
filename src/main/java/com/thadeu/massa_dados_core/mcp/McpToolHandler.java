package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataRequest;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse;
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
 * <p>Registra as ferramentas {@code ddl_to_entity}, {@code identify_unknown_entities}
 * e {@code get_entity_metadata}.
 */
@Component
public class McpToolHandler {

    private final McpDdlToEntityService ddlToEntityService;
    private final McpUnknownEntityService unknownEntityService;
    private final McpEntityMetadataService entityMetadataService;

    public McpToolHandler(McpDdlToEntityService ddlToEntityService,
                          McpUnknownEntityService unknownEntityService,
                          McpEntityMetadataService entityMetadataService) {
        this.ddlToEntityService = ddlToEntityService;
        this.unknownEntityService = unknownEntityService;
        this.entityMetadataService = entityMetadataService;
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
                ),
                new McpSchema.Tool(
                        "get_entity_metadata",
                        "Obtém metadados de uma entidade JPA via reflection",
                        new McpSchema.JsonSchema(
                                "object",
                                java.util.Map.of(
                                        "className", new McpSchema.JsonSchema(
                                                "string",
                                                "Nome completo da classe (ex: com.thadeu.massa_dados_core.domain.Cliente)"
                                        )
                                ),
                                List.of("className")
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
            case "get_entity_metadata" -> {
                String className = (String) arguments.get("className");
                EntityMetadataResponse response = entityMetadataService.getEntityMetadata(
                        new EntityMetadataRequest(className));
                yield response;
            }
            default -> throw new IllegalArgumentException("Ferramenta desconhecida: " + toolName);
        };
    }
}
