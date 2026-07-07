package com.thadeu.massa_dados_core.mcp.dto;

/**
 * DTO de request para a ferramenta {@code identify_unknown_entities}.
 *
 * @param ddlScript Script DDL contendo CREATE TABLE, ALTER TABLE, etc.
 */
public record UnknownEntityRequest(String ddlScript) {
}
