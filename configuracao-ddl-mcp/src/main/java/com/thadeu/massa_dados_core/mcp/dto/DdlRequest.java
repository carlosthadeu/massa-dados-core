package com.thadeu.massa_dados_core.mcp.dto;

/**
 * DTO de requisição para a ferramenta ddl_to_entity.
 *
 * @param ddlScript script DDL contendo CREATE TABLE para conversão
 */
public record DdlRequest(String ddlScript) {}
