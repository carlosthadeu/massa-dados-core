package com.thadeu.massa_dados_core.mcp.dto;

public record RelationshipInfo(
        String fieldName,
        String targetEntity,
        String mappedBy,
        String joinColumn,
        String relationshipType
) {}
