package com.thadeu.massa_dados_core.mcp.dto;

/**
 * DTO de request para a ferramenta {@code ddl_to_entity}.
 *
 * @param ddlScript Script DDL contendo CREATE TABLE, ALTER TABLE, etc.
 */
public record DdlRequest(String ddlScript) {
}
