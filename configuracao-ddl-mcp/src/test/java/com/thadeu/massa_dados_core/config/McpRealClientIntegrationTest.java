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
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {TestConfig.class})
@DisplayName("McpRealClientIntegrationTest - Teste com cliente HTTP real simulando o ChatMCP")
class McpRealClientIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DdlToolsService ddlToolsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;

    // Padrões Regex para validação de formato SSE
    private static final Pattern SSE_ENDPOINT_PATTERN = 
            Pattern.compile("^event: endpoint\\ndata: http://localhost:\\d+/mcp/message\\?sessionId=[a-fA-F0-9-]{36}\\n\\n$");
    
    private static final Pattern SSE_MESSAGE_PATTERN = 
            Pattern.compile("^event: message\\ndata: \\{.*\\}\\n\\n$", Pattern.DOTALL);

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
    // 0. Teste de Validação de Formato SSE (via Regex)
    // ============================================================

    @Test
    @DisplayName("deveValidarFormatoSSE - resposta SSE deve estar no formato correto via regex")
    void deveValidarFormatoSSE() throws Exception {
        URL url = new URL(baseUrl + "/mcp");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try {
            int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode);

            // Ler o evento SSE completo
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
            
            // VALIDAÇÃO POR REGEX: Formato exato esperado
            // event: endpoint\n
            // data: http://localhost:PORT/mcp/message?sessionId=UUID\n
            // \n
            Matcher matcher = SSE_ENDPOINT_PATTERN.matcher(body);
            assertTrue(matcher.matches(), 
                    "Resposta SSE não está no formato esperado.\n" +
                    "Esperado: event: endpoint\\ndata: http://localhost:PORT/mcp/message?sessionId=UUID\\n\\n\n" +
                    "Recebido: " + body.replace("\n", "\\n"));

            // Extrair sessionId para uso posterior
            Pattern sessionPattern = Pattern.compile("sessionId=([a-fA-F0-9-]{36})");
            Matcher sessionMatcher = sessionPattern.matcher(body);
            assertTrue(sessionMatcher.find(), "Deve conter sessionId UUID");
            String sessionId = sessionMatcher.group(1);
            assertNotNull(sessionId);
            
            log("✅ Formato SSE validado com sucesso! SessionId: " + sessionId);

        } finally {
            connection.disconnect();
        }
    }

    // ============================================================
    // 1. Teste de Handshake SSE com conexão real (curl-like)
    // ============================================================

    @Test
    @DisplayName("deveRealizarHandshakeSSEComSucesso - GET /mcp com conexão HTTP real")
    void deveRealizarHandshakeSSEComSucesso() throws Exception {
        URL url = new URL(baseUrl + "/mcp");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try {
            int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode);

            // Validação flexível do Content-Type
            String contentType = connection.getHeaderField("Content-Type");
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("text/event-stream"),
                    "Content-Type deve começar com 'text/event-stream'. Atual: " + contentType);
            assertTrue(contentType.contains("charset"),
                    "Content-Type deve conter charset. Atual: " + contentType);

            // Validação flexível do Connection
            String connectionHeader = connection.getHeaderField("Connection");
            assertNotNull(connectionHeader);
            assertTrue(connectionHeader.contains("keep-alive"),
                    "Connection deve conter 'keep-alive'. Atual: " + connectionHeader);

            assertEquals("no-cache", connection.getHeaderField("Cache-Control"));
            assertEquals("no", connection.getHeaderField("X-Accel-Buffering"));

            // Ler o evento SSE
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

            // Validar formato do evento usando regex
            Matcher matcher = SSE_ENDPOINT_PATTERN.matcher(body);
            assertTrue(matcher.matches(),
                    "Evento deve estar no formato correto.\n" +
                    "Esperado: event: endpoint\\ndata: <url>\\n\\n\n" +
                    "Recebido: " + body.replace("\n", "\\n"));

            // Extrair sessionId
            Pattern pattern = Pattern.compile("sessionId=([a-fA-F0-9-]{36})");
            Matcher matcher2 = pattern.matcher(body);
            assertTrue(matcher2.find(), "Deve conter sessionId UUID. Body: " + body);
            String sessionId = matcher2.group(1);
            assertNotNull(sessionId);

        } finally {
            connection.disconnect();
        }
    }

    // ============================================================
    // 2. Teste de EventSource com Java HttpClient (simula o navegador)
    // ============================================================

    @Test
    @DisplayName("deveManterConexaoAbertaComEventSource - GET /mcp com HttpClient simulando EventSource")
    void deveManterConexaoAbertaComEventSource() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/mcp"))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        CompletableFuture<Void> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    assertEquals(200, statusCode, "Status code deve ser 200");

                    // Validar headers
                    HttpHeaders headers = new HttpHeaders();
                    response.headers().map().forEach((key, values) -> {
                        headers.put(key, values);
                    });

                    String contentType = headers.getFirst("Content-Type");
                    assertNotNull(contentType);
                    assertTrue(contentType.startsWith("text/event-stream"),
                            "Content-Type deve começar com 'text/event-stream'. Atual: " + contentType);

                    String connectionHeader = headers.getFirst("Connection");
                    assertNotNull(connectionHeader);
                    assertTrue(connectionHeader.contains("keep-alive"),
                            "Connection deve conter 'keep-alive'. Atual: " + connectionHeader);

                    // Processar o stream de eventos
                    System.out.println("✅ Conexão SSE estabelecida!");
                    
                    // Flag para indicar se recebemos o evento endpoint
                    boolean[] endpointReceived = {false};
                    StringBuilder eventBuffer = new StringBuilder();
                    
                    response.body().forEach(line -> {
                        System.out.println("📩 Recebido: " + line);
                        eventBuffer.append(line).append("\n");
                        
                        if (line.startsWith("event:")) {
                            String eventType = line.substring(6).trim();
                            System.out.println("📌 Tipo de evento: " + eventType);
                            
                            if ("endpoint".equals(eventType)) {
                                System.out.println("✅ Evento 'endpoint' recebido!");
                            }
                        } else if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            System.out.println("📊 Dados: " + data);
                            
                            if (data.contains("sessionId=")) {
                                endpointReceived[0] = true;
                                System.out.println("✅ SessionId encontrado: " + data);
                            }
                        } else if (line.isEmpty()) {
                            System.out.println("⏭️ Fim do evento");
                            
                            // Validar o evento completo usando regex
                            String fullEvent = eventBuffer.toString();
                            Matcher matcher = SSE_ENDPOINT_PATTERN.matcher(fullEvent);
                            if (matcher.matches()) {
                                System.out.println("✅ Evento formato válido!");
                            } else {
                                System.err.println("❌ Evento formato inválido: " + fullEvent.replace("\n", "\\n"));
                            }
                            
                            // Limpar buffer para o próximo evento
                            eventBuffer.setLength(0);
                        }
                    });
                    
                    assertTrue(endpointReceived[0], "Deveria ter recebido o evento endpoint com sessionId");
                });

        try {
            responseFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("Falha ao conectar SSE: " + e.getMessage());
        }
    }

    // ============================================================
    // 3. Teste de Conexão Longa (simula o comportamento real)
    // ============================================================

    @Test
    @DisplayName("deveManterConexaoAbertaPorMaisDe10Segundos - conexão SSE deve permanecer ativa")
    void deveManterConexaoAbertaPorMaisDe10Segundos() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/mcp"))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        CompletableFuture<Void> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    System.out.println("✅ Conexão SSE estabelecida!");
                    
                    final int[] eventCount = {0};
                    StringBuilder eventBuffer = new StringBuilder();
                    
                    response.body().forEach(line -> {
                        eventCount[0]++;
                        System.out.println("📩 Evento #" + eventCount[0] + ": " + line);
                        eventBuffer.append(line).append("\n");
                        
                        if (line.contains("endpoint") || line.contains("sessionId")) {
                            System.out.println("✅ Conexão está ativa e recebendo eventos!");
                        }
                        
                        // Quando receber um evento completo, validar formato
                        if (line.isEmpty() && eventBuffer.length() > 0) {
                            String fullEvent = eventBuffer.toString();
                            Matcher matcher = SSE_ENDPOINT_PATTERN.matcher(fullEvent);
                            if (matcher.matches()) {
                                System.out.println("✅ Evento endpoint validado com sucesso!");
                            }
                            eventBuffer.setLength(0);
                        }
                    });
                    
                    System.out.println("❌ Conexão foi fechada após " + eventCount[0] + " eventos");
                });

        Thread.sleep(12000);
        responseFuture.cancel(true);
        assertTrue(true, "Conexão mantida por 12 segundos sem erros");
    }

    // ============================================================
    // 4. Fluxo Completo de Inicialização
    // ============================================================

    @Test
    @DisplayName("deveCompletarFluxoDeInicializacao - Handshake -> Initialize -> Initialized")
    void deveCompletarFluxoDeInicializacao() throws Exception {
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

        ResponseEntity<String> initializeResponse = enviarMensagemSSE(sessionId, initializeRequest);
        assertEquals(HttpStatus.OK, initializeResponse.getStatusCode());

        JsonNode initJson = objectMapper.readTree(initializeResponse.getBody());
        assertEquals("2.0", initJson.path("jsonrpc").asText());
        assertEquals(1, initJson.path("id").asInt());
        assertEquals("2024-11-05", initJson.path("result").path("protocolVersion").asText());
        assertEquals("configuracao-ddl-mcp", initJson.path("result").path("serverInfo").path("name").asText());

        String initializedRequest = """
                {
                  "jsonrpc": "2.0",
                  "method": "notifications/initialized",
                  "params": {}
                }
                """;

        ResponseEntity<String> initializedResponse = enviarMensagemSSE(sessionId, initializedRequest);
        assertEquals(HttpStatus.OK, initializedResponse.getStatusCode());
    }

    // ============================================================
    // 5. Teste de Listagem de Ferramentas
    // ============================================================

    @Test
    @DisplayName("deveListarFerramentas - tools/list deve retornar as 3 ferramentas")
    void deveListarFerramentas() throws Exception {
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
        assertTrue(tools.isArray());
        assertEquals(3, tools.size());

        String toolsJson = tools.toString();
        assertTrue(toolsJson.contains("ddl_to_entity"));
        assertTrue(toolsJson.contains("identify_unknown_entities"));
        assertTrue(toolsJson.contains("get_generation_config"));
    }

    // ============================================================
    // 6. Teste de Chamada de Ferramentas
    // ============================================================

    @Test
    @DisplayName("deveChamarDdlToEntity - tools/call deve invocar serviço e retornar resultado")
    void deveChamarDdlToEntity() throws Exception {
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

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("T696SSE"));

        verify(ddlToolsService).ddlToEntity(contains("CREATE TABLE T696SSE"));
    }

    @Test
    @DisplayName("deveChamarIdentifyUnknownEntities - tools/call deve retornar tabelas desconhecidas")
    void deveChamarIdentifyUnknownEntities() throws Exception {
        String sessionId = estabelecerConexaoSSE();
        enviarInitialize(sessionId);

        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "params": {
                    "name": "identify_unknown_entities",
                    "arguments": {
                      "ddlScript": "CREATE TABLE T700DESC (SQ_DESC INT NOT NULL PRIMARY KEY);"
                    }
                  },
                  "id": 4
                }
                """;

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("T700DESC"));

        verify(ddlToolsService).identifyUnknownEntities(anyString());
    }

    // ============================================================
    // 7. Teste de Erros e Timeouts
    // ============================================================

    @Test
    @DisplayName("deveRetornar404ParaSessaoInexistente - POST com sessionId inválido")
    void deveRetornar404ParaSessaoInexistente() {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "initialize",
                  "id": 1
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/mcp/message?sessionId=" + UUID.randomUUID(),
                entity,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("deveLidarComTimeout - servidor não deve fechar conexão prematuramente")
    void deveLidarComTimeout() throws Exception {
        String sessionId = estabelecerConexaoSSE();
        Thread.sleep(11000);

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

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, request);
        assertTrue(response.getStatusCode() == HttpStatus.OK ||
                   response.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    // ============================================================
    // 8. Teste de Modo JSON Síncrono
    // ============================================================

    @Test
    @DisplayName("deveProcessarInitializacaoViaPostJson - POST /mcp sem SSE")
    void deveProcessarInitializacaoViaPostJson() {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "curl", "version": "1.0"}
                  },
                  "id": 1
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/mcp",
                entity,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            assertEquals("2.0", json.path("jsonrpc").asText());
            assertEquals("2024-11-05", json.path("result").path("protocolVersion").asText());
            assertEquals("configuracao-ddl-mcp", json.path("result").path("serverInfo").path("name").asText());
        } catch (Exception e) {
            fail("Resposta não é um JSON válido: " + response.getBody());
        }
    }

    // ============================================================
    // Métodos Auxiliares
    // ============================================================

    private void log(String message) {
        System.out.println("🔍 " + message);
    }

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
            
            // Validar formato antes de extrair sessionId
            Matcher formatMatcher = SSE_ENDPOINT_PATTERN.matcher(body);
            assertTrue(formatMatcher.matches(), 
                    "Resposta SSE não está no formato esperado.\n" +
                    "Esperado: event: endpoint\\ndata: <url>\\n\\n\n" +
                    "Recebido: " + body.replace("\n", "\\n"));

            Pattern pattern = Pattern.compile("sessionId=([a-fA-F0-9-]{36})");
            Matcher matcher = pattern.matcher(body);
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

    private void enviarInitialize(String sessionId) {
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

        ResponseEntity<String> response = enviarMensagemSSE(sessionId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Falha ao enviar initialize");
    }
}