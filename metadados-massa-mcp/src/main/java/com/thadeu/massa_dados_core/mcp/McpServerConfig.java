package com.thadeu.massa_dados_core.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class McpServerConfig {

    @Bean
    public RouterFunction<ServerResponse> mcpRouter(McpToolHandler handler) {
        return route(POST("/mcp"), handler::handle);
    }
}
