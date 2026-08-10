package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

public record GenerationConfigResponse(
        String entityOutputRelativePath,
        String packageName,
        String sourceEntityPackage,
        String sourceEntityRelativePath,
        List<String> existingGeneratedEntities,
        int totalExistingEntities
) {}
