package com.thadeu.massa_dados_core.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpCompileServiceTest {

    @Test
    void deveRetornarFalhaQuandoDiretorioInvalido() {
        McpCompileService service = new McpCompileService();
        ReflectionTestUtils.setField(service, "coreProjectPath", "/caminho/inexistente");

        McpCompileService.CompileResult result = service.compile();

        assertFalse(result.success());
        assertNotNull(result.message());
    }

    @Test
    void deveRetornarFalhaQuandoTimeout() {
        // Simular um diretório que existe mas o comando mvn.cmd não existe
        McpCompileService service = new McpCompileService();
        ReflectionTestUtils.setField(service, "coreProjectPath", System.getProperty("java.io.tmpdir"));

        McpCompileService.CompileResult result = service.compile();

        // Deve falhar porque mvn.cmd não está no PATH ou não existe
        assertFalse(result.success());
        assertNotNull(result.message());
    }
}
