package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataRequest;
import com.thadeu.massa_dados_core.mcp.dto.EntityMetadataResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpEntityMetadataServiceTest {

    private final McpEntityMetadataService service = new McpEntityMetadataService();

    @Test
    void deveLancarExcecaoParaClasseInexistente() {
        EntityMetadataRequest request = new EntityMetadataRequest("br.gov.bnb.domain.entity.ClasseInexistente");
        assertThrows(ClassNotFoundException.class, () -> service.getMetadata(request));
    }

    @Test
    void deveLancarExcecaoParaClasseSemEntity() {
        EntityMetadataRequest request = new EntityMetadataRequest("java.lang.String");
        assertThrows(IllegalArgumentException.class, () -> service.getMetadata(request));
    }

    @Test
    void deveRetornarMetadadosParaPortfolio() throws Exception {
        EntityMetadataRequest request = new EntityMetadataRequest("br.gov.bnb.domain.entity.Portfolio");
        EntityMetadataResponse response = service.getMetadata(request);

        assertNotNull(response);
        assertEquals("Portfolio", response.className());
        assertEquals("T696POAC", response.tableName());
        assertNotNull(response.attributes());
        assertFalse(response.attributes().isEmpty());
    }

    @Test
    void deveRetornarMetadadosParaAcaoEstrategica() throws Exception {
        EntityMetadataRequest request = new EntityMetadataRequest("br.gov.bnb.domain.entity.AcaoEstrategica");
        EntityMetadataResponse response = service.getMetadata(request);

        assertNotNull(response);
        assertEquals("AcaoEstrategica", response.className());
        assertEquals("T696ACES", response.tableName());
        assertNotNull(response.attributes());
        assertFalse(response.attributes().isEmpty());
    }

    @Test
    void deveIdentificarRelacionamentos() throws Exception {
        EntityMetadataRequest request = new EntityMetadataRequest("br.gov.bnb.domain.entity.AcaoEstrategica");
        EntityMetadataResponse response = service.getMetadata(request);

        assertNotNull(response);
        assertNotNull(response.relationships());
        // AcaoEstrategica tem relacionamentos com Portfolio, OrigemAcaoEstrategica, UnidadeOperacional, etc.
        assertFalse(response.relationships().isEmpty());
    }
}
