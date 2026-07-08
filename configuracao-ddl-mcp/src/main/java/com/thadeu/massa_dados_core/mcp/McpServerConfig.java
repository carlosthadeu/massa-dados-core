package com.thadeu.massa_dados_core.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Configuração do endpoint MCP para o servidor configuracao-ddl-mcp.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Expor o endpoint {@code POST /mcp} para comunicação JSON-RPC</li>
 *   <li>Delegar requisições para o {@link McpToolHandler}</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@Configuration
public class McpServerConfig {

    /**
     * Cria a rota para o endpoint MCP.
     *
     * @param handler manipulador de requisições MCP
     * @return função de roteamento que mapeia POST /mcp para o handler
     */
    @Bean
    public RouterFunction<ServerResponse> mcpRouter(McpToolHandler handler) {
        return route(POST("/mcp"), handler::handle);
    }
}
