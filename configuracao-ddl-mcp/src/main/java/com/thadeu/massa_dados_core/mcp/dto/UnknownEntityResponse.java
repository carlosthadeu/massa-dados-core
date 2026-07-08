package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO de resposta para a ferramenta identify_unknown_entities.
 *
 * @param missingTables  lista de nomes de tabelas sem classe Entity correspondente
 * @param missingColumns lista de colunas não reconhecidas nas Entity existentes
 */
public record UnknownEntityResponse(
        List<String> missingTables,
        List<MissingColumnInfo> missingColumns
) {}
