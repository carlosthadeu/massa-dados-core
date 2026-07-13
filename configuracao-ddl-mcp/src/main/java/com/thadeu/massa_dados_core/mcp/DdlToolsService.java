package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.DdlRequest;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityRequest;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DdlToolsService {

    private static final Logger log = LoggerFactory.getLogger(DdlToolsService.class);

    private final McpDdlToEntityService ddlToEntityService;
    private final McpUnknownEntityService unknownEntityService;

    public DdlToolsService(McpDdlToEntityService ddlToEntityService,
                           McpUnknownEntityService unknownEntityService) {
        this.ddlToEntityService = ddlToEntityService;
        this.unknownEntityService = unknownEntityService;
    }

    @Tool(description = "Converte um script DDL em uma classe Entity JPA e salva no projeto do Servidor 2")
    public DdlResponse ddlToEntity(
            @ToolParam(description = "Script DDL (CREATE TABLE, ALTER TABLE, etc.)") String ddlScript) {
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

    @Tool(description = "Compara tabelas/colunas do DDL com as classes Entity existentes e identifica itens nao reconhecidos")
    public UnknownEntityResponse identifyUnknownEntities(
            @ToolParam(description = "Script DDL para analise") String ddlScript) {
        log.info("[identifyUnknownEntities] DDL recebida: {} caracteres", ddlScript.length());
        try {
            UnknownEntityRequest request = new UnknownEntityRequest(ddlScript);
            return unknownEntityService.identify(request);
        } catch (IOException e) {
            log.error("[identifyUnknownEntities] Erro de IO ao identificar entidades", e);
            throw new RuntimeException("Erro ao identificar entidades: " + e.getMessage(), e);
        }
    }
}