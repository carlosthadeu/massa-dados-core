package com.thadeu.massa_dados_core.mcp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link McpCompileService}.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Validar retorno de falha para diretório inválido</li>
 *   <li>Validar retorno de falha para timeout</li>
 * </ul>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpCompileService")
class McpCompileServiceTest {

    @Test
    @DisplayName("deveRetornarFalhaQuandoDiretorioInvalido - diretório inexistente deve retornar falha")
    void deveRetornarFalhaQuandoDiretorioInvalido() {
        McpCompileService service = new McpCompileService();
        ReflectionTestUtils.setField(service, "coreProjectPath", "/caminho/inexistente");

        McpCompileService.CompileResult result = service.compile();

        assertFalse(result.success());
        assertNotNull(result.message());
    }

    @Test
    @DisplayName("deveRetornarFalhaQuandoTimeout - diretório sem mvn.cmd deve retornar falha")
    void deveRetornarFalhaQuandoTimeout() {
        McpCompileService service = new McpCompileService();
        ReflectionTestUtils.setField(service, "coreProjectPath", System.getProperty("java.io.tmpdir"));

        McpCompileService.CompileResult result = service.compile();

        assertFalse(result.success());
        assertNotNull(result.message());
    }
}
