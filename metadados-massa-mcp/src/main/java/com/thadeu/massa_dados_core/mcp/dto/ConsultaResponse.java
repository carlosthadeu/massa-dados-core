package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;
import java.util.Map;

public record ConsultaResponse(
        String resposta,
        List<Map<String, Object>> dados,
        String sqlGerado
) {}
