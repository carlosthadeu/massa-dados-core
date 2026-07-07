package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO de response para a ferramenta {@code ddl_to_entity}.
 *
 * @param entityClassName Nome completo da classe Entity gerada
 * @param entityCode      Código Java gerado
 * @param tableName       Nome da tabela correspondente
 * @param attributes      Lista de atributos gerados
 */
public record DdlResponse(
        String entityClassName,
        String entityCode,
        String tableName,
        List<AttributeInfo> attributes
) {
    /**
     * Informações de um atributo gerado.
     *
     * @param name       Nome do atributo
     * @param javaType   Tipo Java (ex: String, Integer, BigDecimal)
     * @param nullable   Se pode ser nulo
     * @param primaryKey Se é chave primária
     * @param columnName Nome da coluna no banco
     */
    public record AttributeInfo(
            String name,
            String javaType,
            boolean nullable,
            boolean primaryKey,
            String columnName
    ) {}
}
