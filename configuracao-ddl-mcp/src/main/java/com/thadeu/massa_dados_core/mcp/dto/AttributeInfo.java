package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

/**
 * DTO que representa um atributo de uma classe Entity JPA.
 *
 * @param name             nome do campo
 * @param type             tipo Java do campo
 * @param nullable         se o campo permite valores nulos
 * @param isId             se o campo é chave primária
 * @param isGeneratedValue se o campo tem geração automática de valor
 * @param columnName       nome da coluna no banco de dados
 * @param length           tamanho máximo (para String)
 * @param precision        precisão (para BigDecimal)
 * @param scale            escala (para BigDecimal)
 * @param annotations      lista de anotações JPA presentes no campo
 */
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
