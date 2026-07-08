package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

public record AttributeInfo(
        String name,
        String type,
        boolean nullable,
        boolean isId,
        boolean isGeneratedValue,
        String columnName,
        Integer length,
        Integer precision,
        Integer scale,
        List<String> annotations
) {}
