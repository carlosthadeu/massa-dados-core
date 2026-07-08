package com.thadeu.massa_dados_core.mcp.dto;

/**
 * DTO que representa uma coluna não reconhecida em uma tabela existente.
 *
 * @param tableName  nome da tabela
 * @param columnName nome da coluna
 * @param columnType tipo da coluna (genérico)
 * @param nullable   se a coluna permite valores nulos
 */
public record MissingColumnInfo(
        String tableName,
        String columnName,
        String columnType,
        boolean nullable
) {}
