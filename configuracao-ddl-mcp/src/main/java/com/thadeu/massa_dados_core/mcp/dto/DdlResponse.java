package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

public record DdlResponse(
        String entityClassName,
        String entityCode,
        String tableName,
        List<AttributeInfo> attributes,
        boolean compileSuccess,
        String compileMessage
) {}
