package com.thadeu.massa_dados_core.mcp.dto;

/**
 * DTO de request para obter metadados de uma entidade JPA.
 *
 * @param className Nome completo da classe (ex: "com.thadeu.massa_dados_core.domain.Cliente")
 */
public record EntityMetadataRequest(String className) {
}
