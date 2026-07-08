package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class McpMapeamentoSemanticoService {

    private final ObjectMapper objectMapper;

    private JsonNode mapeamentoSemantico;
    private JsonNode sinonimos;

    public McpMapeamentoSemanticoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        // Carregar mapeamento-semantico.json
        try (InputStream is = new ClassPathResource("mapeamento-semantico.json").getInputStream()) {
            mapeamentoSemantico = objectMapper.readTree(is);
        }

        // Carregar sinonimos.json
        try (InputStream is = new ClassPathResource("sinonimos.json").getInputStream()) {
            sinonimos = objectMapper.readTree(is);
        }
    }

    public JsonNode getMapeamentoSemantico() {
        return mapeamentoSemantico;
    }

    public JsonNode getSinonimos() {
        return sinonimos;
    }

    public String getMapeamentoSemanticoJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapeamentoSemantico);
        } catch (IOException e) {
            return "{}";
        }
    }

    public String getSinonimosJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sinonimos);
        } catch (IOException e) {
            return "{}";
        }
    }
}
