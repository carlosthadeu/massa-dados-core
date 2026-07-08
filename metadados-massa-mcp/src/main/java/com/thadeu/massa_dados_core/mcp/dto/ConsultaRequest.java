package com.thadeu.massa_dados_core.mcp.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultaRequest(
        @NotBlank String pergunta
) {}
