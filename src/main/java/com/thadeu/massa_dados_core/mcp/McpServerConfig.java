package com.thadeu.massa_dados_core.mcp;

import io.modelcontextprotocol.spec.McpSchema;
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

    private final McpToolHandler toolHandler;

    public McpServerConfig(McpToolHandler toolHandler) {
        this.toolHandler = toolHandler;
    }

    /**
     * Cria o provedor de transporte para o servidor MCP.
     *
     * @return TransportProvider configurado
     */
    @Bean
    public McpServerTransportProvider mcpServerTransportProvider() {
        // Usa o transporte HTTP padrão do Spring Boot
        McpServerTransportProvider provider = new McpServerTransportProvider();

        // Registrar as ferramentas
        provider.setTools(toolHandler.getTools());

        // Configurar o handler de execução
        provider.setToolHandler(toolHandler::executeTool);

        return provider;
    }
}
