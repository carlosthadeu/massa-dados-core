package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

public record UnknownEntityResponse(
        List<String> missingTables,
        List<MissingColumnInfo> missingColumns
) {}
