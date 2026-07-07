package com.thadeu.massa_dados_core.mcp;

import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do servidor MCP.
 *
 * <p>Registra as ferramentas MCP e configura o transporte HTTP.
 */
@Configuration
public class McpServerConfig {

    /**
     * Cria o provedor de transporte para o servidor MCP.
     *
     * @return TransportProvider configurado
     */
    @Bean
    public McpServerTransportProvider mcpServerTransportProvider() {
        // Usa o transporte HTTP padrão do Spring Boot
        return new McpServerTransportProvider();
    }
}
