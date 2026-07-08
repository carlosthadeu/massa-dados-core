package com.thadeu.massa_dados_core.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpDemandaServiceTest {

    private McpDemandaService service;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        service = new McpDemandaService(null);
        tempDir = Files.createTempDirectory("demandas-test-");
        ReflectionTestUtils.setField(service, "demandasPath", tempDir.toString());
        service.init();
    }

    @Test
    void deveGerarDemanda() {
        String id = service.gerarDemanda(
                "Crie um fórum",
                "Entidade 'forum' não encontrada",
                "Adicionar mapeamento para T696FORU",
                "Tabela T696FORU existe no banco"
        );

        assertNotNull(id);
        assertTrue(id.endsWith(".md"));
    }

    @Test
    void deveListarDemandasPendentes() throws IOException {
        service.gerarDemanda("Teste 1", "Motivo 1", "Sugestão 1", "Detalhes 1");
        service.gerarDemanda("Teste 2", "Motivo 2", "Sugestão 2", "Detalhes 2");

        List<Map<String, Object>> demandas = service.listarDemandasPendentes();

        assertNotNull(demandas);
        assertEquals(2, demandas.size());
    }

    @Test
    void deveResolverDemanda() throws IOException {
        String id = service.gerarDemanda("Teste", "Motivo", "Sugestão", "Detalhes");

        boolean resolvida = service.resolverDemanda(id);
        assertTrue(resolvida);

        // Verificar que não está mais pendente
        List<Map<String, Object>> pendentes = service.listarDemandasPendentes();
        assertTrue(pendentes.isEmpty());
    }

    @Test
    void deveRetornarFalsoParaDemandaInexistente() throws IOException {
        boolean resolvida = service.resolverDemanda("arquivo-inexistente.md");
        assertFalse(resolvida);
    }

    @Test
    void deveDetalharDemanda() throws IOException {
        String id = service.gerarDemanda("Teste detalhe", "Motivo detalhe", "Sugestão detalhe", "Detalhes técnicos");

        Map<String, Object> detalhes = service.detalharDemanda(id);

        assertNotNull(detalhes);
        assertEquals(id, detalhes.get("id"));
        assertEquals("pendentes", detalhes.get("status"));
        assertNotNull(detalhes.get("conteudo"));
    }

    @Test
    void deveRetornarNuloParaDemandaInexistente() throws IOException {
        Map<String, Object> detalhes = service.detalharDemanda("arquivo-inexistente.md");
        assertNull(detalhes);
    }
}
