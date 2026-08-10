package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.GenerationConfigResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class DdlToolsService {

    private static final Logger log = LoggerFactory.getLogger(DdlToolsService.class);

    private final McpDdlToEntityService ddlToEntityService;
    private final McpUnknownEntityService unknownEntityService;
    private final McpGenerationConfigService generationConfigService;

    public DdlToolsService(McpDdlToEntityService ddlToEntityService,
                           McpUnknownEntityService unknownEntityService,
                           McpGenerationConfigService generationConfigService) {
        this.ddlToEntityService = ddlToEntityService;
        this.unknownEntityService = unknownEntityService;
        this.generationConfigService = generationConfigService;
    }

    /**
     * Converte um script DDL em uma classe Entity JPA e salva no projeto
     */
    public DdlResponse ddlToEntity(String ddlScript) {
        log.info("[ddlToEntity] DDL recebida: {} caracteres", ddlScript.length());
        try {
            DdlRequest request = new DdlRequest(ddlScript);
            return ddlToEntityService.processDdl(request);
        } catch (IOException e) {
            log.error("[ddlToEntity] Erro de IO ao processar DDL", e);
            throw new RuntimeException("Erro ao processar DDL: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("[ddlToEntity] Erro de validacao no DDL", e);
            throw new RuntimeException("DDL invalido: " + e.getMessage(), e);
        }
    }

    /**
     * Compara tabelas/colunas do DDL com as classes Entity existentes
     */
    public UnknownEntityResponse identifyUnknownEntities(String ddlScript) {
        log.info("[identifyUnknownEntities] DDL recebida: {} caracteres", ddlScript.length());
        try {
            UnknownEntityRequest request = new UnknownEntityRequest(ddlScript);
            return unknownEntityService.identify(request);
        } catch (IOException e) {
            log.error("[identifyUnknownEntities] Erro de IO ao identificar entidades", e);
            throw new RuntimeException("Erro ao identificar entidades: " + e.getMessage(), e);
        }
    }

    /**
     * Retorna as configurações atuais de geração de entidades JPA
     */
    public GenerationConfigResponse getGenerationConfig() {
        log.info("[getGenerationConfig] Obtendo configuracao de geracao");
        return generationConfigService.getConfig();
    }

    /**
     * Retorna as definições das ferramentas para o MCP.
     * Usado pelo AdaptiveMcpController para listar as ferramentas disponíveis.
     */
    public static List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // Tool 1: ddl_to_entity
        Map<String, Object> tool1 = new LinkedHashMap<>();
        tool1.put("name", "ddl_to_entity");
        tool1.put("description", "Converte um script DDL em uma classe Entity JPA e salva no projeto");
        
        Map<String, Object> inputSchema1 = new LinkedHashMap<>();
        inputSchema1.put("type", "object");
        Map<String, Object> properties1 = new LinkedHashMap<>();
        Map<String, Object> ddlProp1 = new LinkedHashMap<>();
        ddlProp1.put("type", "string");
        ddlProp1.put("description", "Script DDL (CREATE TABLE, ALTER TABLE, etc.)");
        properties1.put("ddlScript", ddlProp1);
        inputSchema1.put("properties", properties1);
        inputSchema1.put("required", Collections.singletonList("ddlScript"));
        tool1.put("inputSchema", inputSchema1);
        tools.add(tool1);
        
        // Tool 2: identify_unknown_entities
        Map<String, Object> tool2 = new LinkedHashMap<>();
        tool2.put("name", "identify_unknown_entities");
        tool2.put("description", "Compara tabelas/colunas do DDL com as classes Entity existentes");
        
        Map<String, Object> inputSchema2 = new LinkedHashMap<>();
        inputSchema2.put("type", "object");
        Map<String, Object> properties2 = new LinkedHashMap<>();
        Map<String, Object> ddlProp2 = new LinkedHashMap<>();
        ddlProp2.put("type", "string");
        ddlProp2.put("description", "Script DDL para análise");
        properties2.put("ddlScript", ddlProp2);
        inputSchema2.put("properties", properties2);
        inputSchema2.put("required", Collections.singletonList("ddlScript"));
        tool2.put("inputSchema", inputSchema2);
        tools.add(tool2);
        
        // Tool 3: get_generation_config
        Map<String, Object> tool3 = new LinkedHashMap<>();
        tool3.put("name", "get_generation_config");
        tool3.put("description", "Retorna as configurações atuais de geração de entidades JPA");
        
        Map<String, Object> inputSchema3 = new LinkedHashMap<>();
        inputSchema3.put("type", "object");
        inputSchema3.put("properties", new LinkedHashMap<>());
        tool3.put("inputSchema", inputSchema3);
        tools.add(tool3);
        
        return tools;
    }
}