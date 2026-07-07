package com.thadeu.massa-dados-core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação Spring Boot para o servidor MCP de geração de massa de dados.
 *
 * <p>Este servidor expõe ferramentas MCP para:
 * <ul>
 *   <li>Obter metadados de entidades JPA via reflection</li>
 *   <li>Converter scripts DDL em classes Entity</li>
 *   <li>Identificar tabelas/colunas sem Entity correspondente</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
