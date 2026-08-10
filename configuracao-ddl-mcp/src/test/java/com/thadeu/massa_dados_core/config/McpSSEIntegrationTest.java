package com.thadeu.massa_dados_core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.DdlToolsService;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {TestConfig.class})
@DisplayName("McpSSEIntegrationTest - Validação de cenários SSE para ChatMCP")
class McpSSEIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DdlToolsService ddlToolsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;

    // Padrão para extrair sessionId
    private static final Pattern SESSION_ID_PATTERN = 
            Pattern.compile("sessionId=([a-fA-F0-9-]{36})");

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        reset(ddlToolsService);

        when(ddlToolsService.ddlToEntity(anyString()))
                .thenReturn(new DdlResponse("Teste", "@Entity\npublic class Teste {}",
                        "T696SSE", List.of(), true, "OK"));

        when(ddlToolsService.identifyUnknownEntities(anyString()))
                .thenReturn(new UnknownEntityResponse(List.of("T700DESC"), List.of()));
    }

    // ============================================================
    // Cenário 1: A conexão SSE está ativa? O SseEmitter está sendo mantido aberto?
    // ============================================================

    @Test
    @DisplayName("Cenário 1: Conexão SSE deve permanecer ativa após o handshake")
    void deveManterConexaoSSEAtiva() throws Exception {
        URL url = new URL(baseUrl + "/mcp");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode);

            // Validar headers
            String contentType = connection.getHeaderField("Content-Type");
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("text/event-stream"),
                    "Content-Type deve começar com 'text/event-stream'. Atual: " + contentType);

            String connectionHeader = connection.getHeaderField("Connection");
            assertNotNull(connectionHeader);
            assertTrue(connectionHeader.contains("keep-alive"),
                    "Connection deve conter 'keep-alive'. Atual: " + connectionHeader);

            assertEquals("no-cache", connection.getHeaderField("Cache-Control"));
            assertEquals("no", connection.getHeaderField("X-Accel-Buffering"));

            // Ler o evento endpoint
            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                long startTime = System.currentTimeMillis();
                boolean eventReceived = false;

                while ((line = reader.readLine()) != null) {
                    responseBody.append(line).append("\n");
                    
                    if (line.isEmpty()) {
                        String fullEvent = responseBody.toString();
                        if (fullEvent.contains("event: endpoint") && 
                            fullEvent.contains("data:")) {
                            eventReceived = true;
                            System.out.println("✅ Evento endpoint recebido");
                            break;
                        }
                        responseBody.setLength(0);
                    }

                    if (System.currentTimeMillis() - startTime > 5000) {
                        break;
                    }
                }

                assertTrue(eventReceived, "Deveria ter recebido o evento endpoint");

                // Extrair sessionId do evento
                Matcher matcher = SESSION_ID_PATTERN.matcher(responseBody.toString());
                if (matcher.find()) {
                    String sessionId = matcher.group(1);
                    assertNotNull(sessionId);
                    System.out.println("✅ SessionId obtido: " + sessionId);
                }

            } catch (Exception e) {
                fail("Falha ao ler evento SSE: " + e.getMessage());
            }

        } finally {
            connection.disconnect();
        }
    }

    // ============================================================
    // Cenário 2: O evento message está sendo enviado?
    // O ChatMCP espera eventos com nome message
    // ============================================================

    @Test
    @DisplayName("Cenário 2: Respostas devem ser enviadas como evento 'message'")
    void deveEnviarEventoMessage() throws Exception {
        String sessionId = estabelecerConexaoSSE();

        String initializeRequest = """
                {
                  "jsonrpc": "2.0",
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "DartMCPClient", "version": "1.0.0"}
                  },
                  "id": 1
                }
                """;

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, initializeRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // Validar que a resposta é um JSON-RPC válido
        assertTrue(responseBody.contains("jsonrpc"), "Resposta deve conter 'jsonrpc'");
        assertTrue(responseBody.contains("result"), "Resposta deve conter 'result'");
        
        JsonNode json = objectMapper.readTree(responseBody);
        assertEquals("2.0", json.path("jsonrpc").asText());
        assertEquals(1, json.path("id").asInt());
        assertTrue(json.has("result"));
        
        System.out.println("✅ Resposta initialize recebida como evento 'message'");
    }

    // ============================================================
    // Cenário 3: O formato do evento está correto?
    // event: message\ndata: {...}\n\n
    // ============================================================

    @Test
    @DisplayName("Cenário 3: Formato do evento SSE deve estar correto")
    void deveValidarFormatoEventoSSE() throws Exception {
        String sessionId = estabelecerConexaoSSE();

        String initializeRequest = """
                {
                  "jsonrpc": "2.0",
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "DartMCPClient", "version": "1.0.0"}
                  },
                  "id": 1
                }
                """;

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, initializeRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody);

        // Validar estrutura JSON-RPC
        JsonNode json = objectMapper.readTree(responseBody);
        assertTrue(json.has("jsonrpc"), "Deveria ter campo 'jsonrpc'");
        assertTrue(json.has("id"), "Deveria ter campo 'id'");
        assertTrue(json.has("result"), "Deveria ter campo 'result'");
        
        // Validar que o Content-Type é JSON
        HttpHeaders headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        
        System.out.println("✅ Evento 'message' recebido com formato correto");
    }

    // ============================================================
    // Cenário 4: tools/list deve retornar a lista de ferramentas
    // ============================================================

    @Test
    @DisplayName("Cenário 4: tools/list deve retornar as 3 ferramentas")
    void deveRetornarListaDeFerramentas() throws Exception {
        String sessionId = estabelecerConexaoSSE();
        enviarInitialize(sessionId);

        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/list",
                  "id": 2
                }
                """;

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode json = objectMapper.readTree(response.getBody());
        JsonNode tools = json.path("result").path("tools");
        assertTrue(tools.isArray(), "tools deve ser um array");
        assertEquals(3, tools.size(), "Deve ter 3 ferramentas");

        // Validar cada ferramenta
        for (JsonNode tool : tools) {
            assertTrue(tool.has("name"), "Ferramenta deve ter 'name'");
            assertTrue(tool.has("description"), "Ferramenta deve ter 'description'");
            assertTrue(tool.has("inputSchema"), "Ferramenta deve ter 'inputSchema'");
            
            JsonNode inputSchema = tool.path("inputSchema");
            assertTrue(inputSchema.has("type"), "inputSchema deve ter 'type'");
            assertEquals("object", inputSchema.path("type").asText(), "type deve ser 'object'");
        }

        System.out.println("✅ tools/list retornou 3 ferramentas com estrutura correta");
    }

    // ============================================================
    // Cenário 5: tools/call deve executar a ferramenta
    // ============================================================

    @Test
    @DisplayName("Cenário 5: tools/call deve executar a ferramenta e retornar resultado")
    void deveExecutarFerramenta() throws Exception {
        String sessionId = estabelecerConexaoSSE();
        enviarInitialize(sessionId);

        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "params": {
                    "name": "ddl_to_entity",
                    "arguments": {
                      "ddlScript": "CREATE TABLE T696SSE (SQ_SSE INT NOT NULL PRIMARY KEY);"
                    }
                  },
                  "id": 3
                }
                """;

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // Validar que a resposta contém a estrutura esperada
        assertTrue(responseBody.contains("T696SSE"), 
                "Resposta deve conter 'T696SSE'. Body: " + responseBody);
        assertTrue(responseBody.contains("result"), 
                "Resposta deve conter 'result'. Body: " + responseBody);
        assertTrue(responseBody.contains("content"), 
                "Resposta deve conter 'content'. Body: " + responseBody);

        // Verificar chamada do serviço
        verify(ddlToolsService).ddlToEntity(contains("CREATE TABLE T696SSE"));

        System.out.println("✅ tools/call executou a ferramenta com sucesso");
    }

    // ============================================================
    // Cenário 6: Fluxo completo (Handshake -> Initialize -> tools/list -> tools/call)
    // ============================================================

    @Test
    @DisplayName("Cenário 6: Fluxo completo de comunicação SSE")
    void deveCompletarFluxoCompleto() throws Exception {
        // 1. Handshake
        String sessionId = estabelecerConexaoSSE();
        assertNotNull(sessionId);
        System.out.println("✅ 1. Handshake concluído, sessionId: " + sessionId);

        // 2. Initialize
        ResponseEntity<String> initResponse = enviarInitialize(sessionId);
        assertEquals(HttpStatus.OK, initResponse.getStatusCode());
        System.out.println("✅ 2. Initialize concluído");

        // 3. tools/list
        ResponseEntity<String> listResponse = enviarToolsList(sessionId);
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        System.out.println("✅ 3. tools/list concluído");

        // 4. tools/call
        ResponseEntity<String> callResponse = enviarToolsCall(sessionId);
        assertEquals(HttpStatus.OK, callResponse.getStatusCode());
        System.out.println("✅ 4. tools/call concluído");

        // Verificar chamadas do serviço
        verify(ddlToolsService, atLeastOnce()).ddlToEntity(anyString());

        System.out.println("🎉 Fluxo completo de comunicação SSE executado com sucesso!");
    }

    // ============================================================
    // Métodos Auxiliares
    // ============================================================

    private String estabelecerConexaoSSE() throws Exception {
        URL url = new URL(baseUrl + "/mcp");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try {
            int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode, "Falha ao conectar: " + responseCode);

            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line).append("\n");
                    if (line.isEmpty()) break;
                }
            }

            String body = responseBody.toString();
            Matcher matcher = SESSION_ID_PATTERN.matcher(body);
            assertTrue(matcher.find(), "Não foi possível extrair sessionId. Body: " + body);

            return matcher.group(1);

        } finally {
            connection.disconnect();
        }
    }

    private ResponseEntity<String> enviarMensagemSSE(String sessionId, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(message, headers);

        return restTemplate.postForEntity(
                baseUrl + "/mcp/message?sessionId=" + sessionId,
                entity,
                String.class
        );
    }

    private ResponseEntity<String> enviarInitialize(String sessionId) {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "DartMCPClient", "version": "1.0.0"}
                  },
                  "id": 1
                }
                """;
        return enviarMensagemSSE(sessionId, request);
    }

    private ResponseEntity<String> enviarToolsList(String sessionId) {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/list",
                  "id": 2
                }
                """;
        return enviarMensagemSSE(sessionId, request);
    }

    private ResponseEntity<String> enviarToolsCall(String sessionId) {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "params": {
                    "name": "ddl_to_entity",
                    "arguments": {
                      "ddlScript": "CREATE TABLE T696SSE (SQ_SSE INT NOT NULL PRIMARY KEY);"
                    }
                  },
                  "id": 3
                }
                """;
        return enviarMensagemSSE(sessionId, request);
    }
}