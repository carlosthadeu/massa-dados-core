package com.thadeu.massa_dados_core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de contexto da aplicação configuracao-ddl-mcp.
 *
 * <p>Verifica se o Spring Boot consegue carregar o contexto da aplicação
 * com todas as dependências e configurações.</p>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@SpringBootTest
@DisplayName("App")
class AppTest {

    @Test
    @DisplayName("deveCarregarContexto - contexto Spring deve carregar sem erros")
    void contextLoads() {
    }
}
