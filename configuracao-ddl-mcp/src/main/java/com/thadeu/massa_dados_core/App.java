package com.thadeu.massa_dados_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal do servidor configuracao-ddl-mcp.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Inicializar o Spring Boot com scan nos pacotes de entidades JPA</li>
 *   <li>Expor endpoints MCP adaptativos para conversao de DDL e identificacao de entidades</li>
 * </ul>
 *
 * <p>O servidor suporta dois protocolos simultaneamente:
 * <ul>
 *   <li><strong>SSE (Server-Sent Events)</strong> - Para clientes como ChatMCP (type: streamable)</li>
 *   <li><strong>JSON-RPC Stateless</strong> - Para clientes como curl, Postman, etc.</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 2.0
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
}