package com.thadeu.massa_dados_core.mcp.dto;

import java.util.List;

public record EntityMetadataResponse(
        String className,
        String tableName,
        List<AttributeInfo> attributes,
        List<RelationshipInfo> relationships
) {}
