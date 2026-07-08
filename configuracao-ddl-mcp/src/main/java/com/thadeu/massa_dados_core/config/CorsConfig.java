package com.thadeu.massa_dados_core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de CORS para o servidor configuracao-ddl-mcp.
 *
 * <p>Permite requisições de qualquer origem para o endpoint MCP,
 * necessário para o Continue chamar via HTTP.</p>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Configura as regras de CORS permitindo qualquer origem.
     *
     * @param registry registro de configuração CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
