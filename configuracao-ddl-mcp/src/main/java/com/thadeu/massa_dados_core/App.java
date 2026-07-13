package com.thadeu.massa_dados_core;

import com.thadeu.massa_dados_core.mcp.DdlToolsService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Classe principal do servidor configuracao-ddl-mcp.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Inicializar o Spring Boot com scan nos pacotes de entidades JPA</li>
 *   <li>Expor o endpoint MCP para conversao de DDL e identificacao de entidades</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.1
 */
@SpringBootApplication(scanBasePackages = {
    "com.thadeu.massa_dados_core",
    "br.gov.bnb.domain.entity"
})
public class App {

    /**
     * Ponto de entrada da aplicacao.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    ToolCallbackProvider tools(DdlToolsService ddlToolsService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(ddlToolsService)
                .build();
    }
}