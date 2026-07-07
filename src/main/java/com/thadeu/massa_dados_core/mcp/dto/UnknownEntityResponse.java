package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO de response para a ferramenta {@code identify_unknown_entities}.
 *
 * @param missingTables  Lista de nomes de tabelas que não possuem classe Entity correspondente
 * @param missingColumns Lista de colunas que não possuem atributo correspondente na Entity
 */
public record UnknownEntityResponse(
        List<String> missingTables,
        List<MissingColumnInfo> missingColumns
) {
    /**
     * Informações de uma coluna faltante.
     *
     * @param tableName  Nome da tabela
     * @param columnName Nome da coluna
     * @param columnType Tipo da coluna no banco
     */
    public record MissingColumnInfo(
            String tableName,
            String columnName,
            String columnType
    ) {}
}
