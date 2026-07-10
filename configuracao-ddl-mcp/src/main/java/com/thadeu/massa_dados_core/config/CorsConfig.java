package com.thadeu.massa_dados_core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Configura as regras de CORS permitindo qualquer origem.
     *
     * @param registry registro de configuração CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("[addCorsMappings] Configurando CORS para permitir todas as origens");
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
