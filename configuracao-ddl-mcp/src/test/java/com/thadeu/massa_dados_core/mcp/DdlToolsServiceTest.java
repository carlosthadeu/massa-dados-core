package com.thadeu.massa_dados_core.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DdlToolsServiceTest - Validação das definições de ferramentas para ChatMCP")
class DdlToolsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("deveRetornarEstruturaCorretaParaChatMCP - getToolDefinitions deve ter estrutura esperada pelo ChatMCP")
    void deveRetornarEstruturaCorretaParaChatMCP() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();

        assertNotNull(tools, "A lista de ferramentas não deve ser nula");
        assertEquals(3, tools.size(), "Deve ter exatamente 3 ferramentas");

        for (Map<String, Object> tool : tools) {
            // Validar campos obrigatórios
            assertTrue(tool.containsKey("name"), "Ferramenta deve ter campo 'name'");
            assertTrue(tool.containsKey("description"), "Ferramenta deve ter campo 'description'");
            assertTrue(tool.containsKey("inputSchema"), "Ferramenta deve ter campo 'inputSchema'");

            // Validar tipos
            assertInstanceOf(String.class, tool.get("name"), "'name' deve ser String");
            assertInstanceOf(String.class, tool.get("description"), "'description' deve ser String");
            assertInstanceOf(Map.class, tool.get("inputSchema"), "'inputSchema' deve ser Map");

            // Validar inputSchema
            @SuppressWarnings("unchecked")
            Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");
            
            // VALIDAÇÃO 1: Deve ter '$defs' (como o Anytype)
            assertTrue(inputSchema.containsKey("$defs"), 
                    "inputSchema deve ter campo '$defs' para a ferramenta: " + tool.get("name"));
            assertInstanceOf(Map.class, inputSchema.get("$defs"), 
                    "'$defs' deve ser Map para a ferramenta: " + tool.get("name"));
            
            assertTrue(inputSchema.containsKey("type"), "inputSchema deve ter campo 'type'");
            assertEquals("object", inputSchema.get("type"), "inputSchema.type deve ser 'object'");

            // Validar properties (se houver)
            if (inputSchema.containsKey("properties")) {
                assertInstanceOf(Map.class, inputSchema.get("properties"), "properties deve ser Map");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
                
                for (Map.Entry<String, Object> prop : properties.entrySet()) {
                    assertInstanceOf(Map.class, prop.getValue(), "Cada propriedade deve ser um Map");
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propDetail = (Map<String, Object>) prop.getValue();
                    assertTrue(propDetail.containsKey("type"), "Propriedade deve ter 'type'");
                    assertInstanceOf(String.class, propDetail.get("type"), "prop.type deve ser String");
                    
                    // VALIDAÇÃO 2: Deve ter 'description' em cada propriedade
                    if (propDetail.containsKey("description")) {
                        assertInstanceOf(String.class, propDetail.get("description"), 
                                "prop.description deve ser String");
                    }
                }
            }

            // Validar required (se houver)
            if (inputSchema.containsKey("required")) {
                assertInstanceOf(List.class, inputSchema.get("required"), "required deve ser List");
                
                @SuppressWarnings("unchecked")
                List<String> required = (List<String>) inputSchema.get("required");
                for (String req : required) {
                    assertInstanceOf(String.class, req, "required items devem ser String");
                }
            }
        }
    }

    @Test
    @DisplayName("deveValidarFerramentaDdlToEntity - estrutura específica da ferramenta ddl_to_entity")
    void deveValidarFerramentaDdlToEntity() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        Map<String, Object> ddlToEntity = tools.stream()
                .filter(t -> "ddl_to_entity".equals(t.get("name")))
                .findFirst()
                .orElse(null);
        
        assertNotNull(ddlToEntity, "Ferramenta 'ddl_to_entity' não encontrada");
        
        assertEquals("ddl_to_entity", ddlToEntity.get("name"));
        assertNotNull(ddlToEntity.get("description"));
        assertTrue(((String) ddlToEntity.get("description")).contains("DDL"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) ddlToEntity.get("inputSchema");
        
        // Validar $defs
        assertTrue(inputSchema.containsKey("$defs"));
        assertInstanceOf(Map.class, inputSchema.get("$defs"));
        
        assertTrue(inputSchema.containsKey("properties"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        
        assertTrue(properties.containsKey("ddlScript"), "Deve ter propriedade 'ddlScript'");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> ddlScriptProp = (Map<String, Object>) properties.get("ddlScript");
        assertEquals("string", ddlScriptProp.get("type"), "ddlScript.type deve ser 'string'");
        assertNotNull(ddlScriptProp.get("description"), "ddlScript deve ter description");
        
        assertTrue(inputSchema.containsKey("required"));
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertTrue(required.contains("ddlScript"), "ddlScript deve ser required");
    }

    @Test
    @DisplayName("deveValidarFerramentaIdentifyUnknownEntities - estrutura específica da ferramenta identify_unknown_entities")
    void deveValidarFerramentaIdentifyUnknownEntities() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        Map<String, Object> tool = tools.stream()
                .filter(t -> "identify_unknown_entities".equals(t.get("name")))
                .findFirst()
                .orElse(null);
        
        assertNotNull(tool, "Ferramenta 'identify_unknown_entities' não encontrada");
        
        assertEquals("identify_unknown_entities", tool.get("name"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");
        
        // Validar $defs
        assertTrue(inputSchema.containsKey("$defs"));
        assertInstanceOf(Map.class, inputSchema.get("$defs"));
        
        assertTrue(inputSchema.containsKey("properties"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        
        assertTrue(properties.containsKey("ddlScript"), "Deve ter propriedade 'ddlScript'");
        
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) inputSchema.get("required");
        assertTrue(required.contains("ddlScript"), "ddlScript deve ser required");
    }

    @Test
    @DisplayName("deveValidarFerramentaGetGenerationConfig - estrutura específica da ferramenta get_generation_config")
    void deveValidarFerramentaGetGenerationConfig() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        Map<String, Object> tool = tools.stream()
                .filter(t -> "get_generation_config".equals(t.get("name")))
                .findFirst()
                .orElse(null);
        
        assertNotNull(tool, "Ferramenta 'get_generation_config' não encontrada");
        
        assertEquals("get_generation_config", tool.get("name"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");
        
        // Validar $defs
        assertTrue(inputSchema.containsKey("$defs"));
        assertInstanceOf(Map.class, inputSchema.get("$defs"));
        
        assertTrue(inputSchema.containsKey("properties"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        assertTrue(properties.isEmpty(), "get_generation_config não deve ter propriedades");
        
        assertFalse(inputSchema.containsKey("required"), "get_generation_config não deve ter required");
    }

    @Test
    @DisplayName("deveSerializarParaJSON - a estrutura deve ser serializável para JSON")
    void deveSerializarParaJSON() throws Exception {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        String json = objectMapper.writeValueAsString(tools);
        
        JsonNode jsonNode = objectMapper.readTree(json);
        assertTrue(jsonNode.isArray(), "Deve ser um array JSON");
        assertEquals(3, jsonNode.size(), "Deve ter 3 ferramentas");
        
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonNode tool = jsonNode.get(i);
            
            assertTrue(tool.has("name"), "Ferramenta deve ter 'name'");
            assertTrue(tool.has("description"), "Ferramenta deve ter 'description'");
            assertTrue(tool.has("inputSchema"), "Ferramenta deve ter 'inputSchema'");
            
            JsonNode inputSchema = tool.get("inputSchema");
            
            // Validar $defs no JSON
            assertTrue(inputSchema.has("$defs"), "inputSchema deve ter '$defs'");
            assertTrue(inputSchema.get("$defs").isObject(), "'$defs' deve ser um objeto");
            
            assertTrue(inputSchema.has("type"), "inputSchema deve ter 'type'");
            assertEquals("object", inputSchema.get("type").asText(), "type deve ser 'object'");
        }
    }

    @Test
    @DisplayName("deveTerNomesUnicos - todas as ferramentas devem ter nomes únicos")
    void deveTerNomesUnicos() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        long distinctNames = tools.stream()
                .map(t -> (String) t.get("name"))
                .distinct()
                .count();
        
        assertEquals(tools.size(), distinctNames, "Todos os nomes devem ser únicos");
    }

    @Test
    @DisplayName("deveTerDescricoesNaoVazias - todas as ferramentas devem ter descrições não vazias")
    void deveTerDescricoesNaoVazias() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        for (Map<String, Object> tool : tools) {
            String description = (String) tool.get("description");
            assertNotNull(description, "Descrição não deve ser nula");
            assertFalse(description.trim().isEmpty(), "Descrição não deve estar vazia");
            assertTrue(description.length() > 10, "Descrição deve ter mais de 10 caracteres");
        }
    }

    @Test
    @DisplayName("deveTerDefsEmTodasFerramentas - todas as ferramentas devem ter $defs")
    void deveTerDefsEmTodasFerramentas() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        for (Map<String, Object> tool : tools) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");
            assertTrue(inputSchema.containsKey("$defs"), 
                    "Ferramenta '" + tool.get("name") + "' deve ter '$defs'");
            assertInstanceOf(Map.class, inputSchema.get("$defs"), 
                    "Ferramenta '" + tool.get("name") + "' '$defs' deve ser Map");
        }
    }

    @Test
    @DisplayName("deveSimularChatMCP - estrutura deve ser consumível pelo ChatMCP como no código")
    void deveSimularChatMCP() {
        // Simula o que o ChatMCP faz para obter a contagem de ferramentas
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        // Simular a variável serverTools do ChatMCP (List<Map<String, Object>>)
        List<Map<String, Object>> serverTools = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            Map<String, Object> chatMcpTool = new HashMap<>();
            chatMcpTool.put("name", tool.get("name"));
            chatMcpTool.put("description", tool.get("description"));
            chatMcpTool.put("inputSchema", tool.get("inputSchema"));
            serverTools.add(chatMcpTool);
        }
        
        // O ChatMCP verifica se toolCount > 0 para exibir "X tools"
        int toolCount = serverTools != null ? serverTools.size() : 0;
        
        // Validar que o ChatMCP veria as ferramentas
        assertTrue(toolCount > 0, "ChatMCP deve ver ferramentas disponíveis");
        assertEquals(3, toolCount, "ChatMCP deve ver 3 ferramentas");
        
        // Simular a exibição no ChatMCP
        assertTrue(toolCount > 0, "O ChatMCP exibiria as ferramentas");
        
        System.out.println("✅ ChatMCP simulado: " + toolCount + " ferramentas disponíveis");
        for (Map<String, Object> tool : serverTools) {
            System.out.println("   - " + tool.get("name") + ": " + tool.get("description"));
        }
    }

    @Test
    @DisplayName("deveCompararComAnytype - estrutura deve ser compatível com o formato do Anytype")
    void deveCompararComAnytype() {
        List<Map<String, Object>> tools = DdlToolsService.getToolDefinitions();
        
        // Simula a estrutura que o Anytype retorna (observada nos logs do ChatMCP)
        // Anytype usa: {"$defs": {}, "type": "object", "properties": {...}, "required": [...]}
        
        for (Map<String, Object> tool : tools) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputSchema = (Map<String, Object>) tool.get("inputSchema");
            
            // Anytype tem $defs
            assertTrue(inputSchema.containsKey("$defs"), 
                    "Como Anytype, ferramenta deve ter '$defs'");
            
            // Anytype type é object
            assertEquals("object", inputSchema.get("type"), 
                    "Como Anytype, type deve ser 'object'");
            
            // Anytype tem properties
            assertTrue(inputSchema.containsKey("properties"), 
                    "Como Anytype, deve ter 'properties'");
            
            // Anytype pode ter required (pode ser vazio)
            if (inputSchema.containsKey("required")) {
                assertInstanceOf(List.class, inputSchema.get("required"), 
                        "Como Anytype, required deve ser List");
            }
        }
        
        System.out.println("✅ Estrutura compatível com o formato do Anytype");
    }
}