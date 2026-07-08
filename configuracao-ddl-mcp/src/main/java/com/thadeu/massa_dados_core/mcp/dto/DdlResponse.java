package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO de resposta para a ferramenta ddl_to_entity.
 *
 * @param entityClassName nome da classe Entity gerada
 * @param entityCode      código Java completo da Entity
 * @param tableName       nome da tabela no banco de dados
 * @param attributes      lista de atributos da Entity
 * @param compileSuccess  true se a compilação foi bem-sucedida
 * @param compileMessage  mensagem de saída ou erro da compilação
 */
public record DdlResponse(
        String entityClassName,
        String entityCode,
        String tableName,
        List<AttributeInfo> attributes,
        boolean compileSuccess,
        String compileMessage
) {}
