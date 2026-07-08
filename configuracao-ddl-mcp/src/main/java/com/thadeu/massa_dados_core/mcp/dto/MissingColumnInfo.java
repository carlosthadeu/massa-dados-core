package com.thadeu.massa_dados_core.mcp.dto;

public record MissingColumnInfo(
        String tableName,
        String columnName,
        String columnType,
        boolean nullable
) {}
