package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO de resposta para a ferramenta simulate_ddl_generation (dry-run).
 */
public record SimulateResponse(
        String tableName,
        String className,
        String entityCode,
        List<AttributeInfo> attributes,
        boolean wouldCreateNew,
        List<String> missingColumns
) {}
