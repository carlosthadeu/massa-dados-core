package com.thadeu.massa_dados_core.mcp.dto;

public record Filtro(
        String atributo,
        String operador,
        Object valor
) {}
