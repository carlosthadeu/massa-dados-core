package com.thadeu.massa_dados_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal do servidor configuracao-ddl-mcp.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Inicializar o Spring Boot com scan nos pacotes de entidades JPA</li>
 *   <li>Expor o endpoint MCP para conversão de DDL e identificação de entidades</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@SpringBootApplication(scanBasePackages = {
    "com.thadeu.massa_dados_core",
    "br.gov.bnb.domain.entity"
})
public class App {

    /**
     * Ponto de entrada da aplicação.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
