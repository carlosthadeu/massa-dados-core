package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Configuração das ferramentas MCP usando o Spring AI MCP Server.
 *
 * <p>Registra as ferramentas {@code ddl_to_entity} e {@code identify_unknown_entities}
 * como {@link ToolCallback} para que o Spring AI exponha automaticamente via MCP.</p>
 *
 * @author Thadeu Garrido
 * @version 2.0
 */
@Configuration
public class McpToolHandler {

    private static final Logger log = LoggerFactory.getLogger(McpToolHandler.class);

    private final ObjectMapper objectMapper;
    private final McpDdlToEntityService ddlToEntityService;
    private final McpUnknownEntityService unknownEntityService;

    /**
     * Construtor com injeção de dependências.
     *
     * @param objectMapper          serializador JSON
     * @param ddlToEntityService    serviço de conversão DDL para Entity
     * @param unknownEntityService  serviço de identificação de entidades desconhecidas
     */
    public McpToolHandler(ObjectMapper objectMapper,
                          McpDdlToEntityService ddlToEntityService,
                          McpUnknownEntityService unknownEntityService) {
        this.objectMapper = objectMapper;
        this.ddlToEntityService = ddlToEntityService;
        this.unknownEntityService = unknownEntityService;
    }

    /**
     * Registra a ferramenta ddl_to_entity.
     *
     * @return ToolCallback para a ferramenta ddl_to_entity
     */
    @Bean
    public ToolCallback ddlToEntityTool() {
        log.info("[ddlToEntityTool] Registrando ferramenta ddl_to_entity");
        return ToolCallbacks.from("ddl_to_entity",
                "Converte um script DDL em uma classe Entity JPA e salva no projeto do Servidor 2",
                (Function<DdlRequest, DdlResponse>) request -> {
                    log.info("[ddlToEntityTool] Processando DDL");
                    try {
                        DdlResponse response = ddlToEntityService.processDdl(request);
                        log.info("[ddlToEntityTool] Entity gerada: {}", response.entityClassName());
                        return response;
                    } catch (Exception e) {
                        log.error("[ddlToEntityTool] Erro ao processar DDL", e);
                        throw new RuntimeException("Error processing DDL: " + e.getMessage(), e);
                    }
                });
    }

    /**
     * Registra a ferramenta identify_unknown_entities.
     *
     * @return ToolCallback para a ferramenta identify_unknown_entities
     */
    @Bean
    public ToolCallback identifyUnknownEntitiesTool() {
        log.info("[identifyUnknownEntitiesTool] Registrando ferramenta identify_unknown_entities");
        return ToolCallbacks.from("identify_unknown_entities",
                "Compara tabelas/colunas do DDL com as classes Entity existentes e identifica itens não reconhecidos",
                (Function<UnknownEntityRequest, UnknownEntityResponse>) request -> {
                    log.info("[identifyUnknownEntitiesTool] Identificando entidades desconhecidas");
                    try {
                        UnknownEntityResponse response = unknownEntityService.identify(request);
                        log.info("[identifyUnknownEntitiesTool] Tabelas faltantes: {}, Colunas faltantes: {}",
                                response.missingTables().size(), response.missingColumns().size());
                        return response;
                    } catch (Exception e) {
                        log.error("[identifyUnknownEntitiesTool] Erro ao identificar entidades", e);
                        throw new RuntimeException("Error identifying unknown entities: " + e.getMessage(), e);
                    }
                });
    }
}
