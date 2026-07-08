package com.thadeu.massa_dados_core.mcp.dto;

/**
 * DTO de requisição para a ferramenta identify_unknown_entities.
 *
 * @param ddlScript script DDL para análise de tabelas/colunas não reconhecidas
 */
public record UnknownEntityRequest(String ddlScript) {}
