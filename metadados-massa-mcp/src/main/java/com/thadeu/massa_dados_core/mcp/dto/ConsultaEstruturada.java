package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

public record ConsultaEstruturada(
        String comando,
        String entidade,
        List<Filtro> filtros,
        String agregacao,
        String atributoAgregacao,
        Ordenacao ordenacao,
        Integer limite
) {}
