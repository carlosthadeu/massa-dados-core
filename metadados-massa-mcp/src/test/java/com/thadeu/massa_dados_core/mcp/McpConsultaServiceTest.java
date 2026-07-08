package com.thadeu.massa_dados_core.mcp;

import com.thadeu.massa_dados_core.mcp.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class McpConsultaServiceTest {

    @Mock
    private McpMapeamentoSemanticoService mapeamentoSemanticoService;

    @InjectMocks
    private McpConsultaService consultaService;

    @Test
    void deveRetornarErroParaEntidadeInexistente() {
        ConsultaEstruturada consulta = new ConsultaEstruturada(
                "consultar",
                "entidade_inexistente",
                List.of(),
                null,
                null,
                null,
                null
        );

        ConsultaResponse response = consultaService.consultar(consulta);

        assertNotNull(response);
        assertTrue(response.resposta().contains("não encontrada"));
    }

    @Test
    void deveRetornarErroParaAtributoInexistente() {
        ConsultaEstruturada consulta = new ConsultaEstruturada(
                "consultar",
                "acao_estrategica",
                List.of(new Filtro("atributo_inexistente", "=", "valor")),
                null,
                null,
                null,
                null
        );

        ConsultaResponse response = consultaService.consultar(consulta);

        assertNotNull(response);
        assertTrue(response.resposta().contains("não encontrado"));
    }

    @Test
    void deveRetornarErroParaComandoInvalido() {
        ConsultaEstruturada consulta = new ConsultaEstruturada(
                "comando_invalido",
                "acao_estrategica",
                List.of(),
                null,
                null,
                null,
                null
        );

        ConsultaResponse response = consultaService.consultar(consulta);

        assertNotNull(response);
        assertTrue(response.resposta().contains("Erro"));
    }
}
