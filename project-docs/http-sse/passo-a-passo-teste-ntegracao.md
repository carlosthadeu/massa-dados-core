# 🧪 Passo a Passo — Teste de Integração SSE para compatibilidade com EventFlux (ChatMCP)

> **Data:** 2026-08-08
> **Classe ativa:** `McpController.java`
> **Problema:** O `SseEmitter` do Spring Boot (Servlet API) empacota os buffers com `\n` (LF), enquanto o EventFlux (Dart) espera eventos SSE separados por `\r\n` (CRLF) — causando `EventFluxException` e timeout do `endpoint` no ChatMCP.
> **Objetivo:** Criar um teste de integração que reflita o comportamento esperado pela ferramenta (ChatMCP via EventFlux) e sirva de prova para a correção.

---

## 1. Contexto e Problema

### 1.1 O fluxo de conexão do ChatMCP (EventFlux)

O `sse_client.dart` do ChatMCP conecta ao servidor MCP da seguinte forma:

| Etapa | Ação | Endpoint |
|-------|------|----------|
| 1 | `EventFlux.connect(GET)` | `GET /mcp` (text/event-stream) |
| 2 | Recebe evento `endpoint` | `event: endpoint\ndata: /mcp/message?sessionId=...\n\n` |
| 3 | Envia `initialize` | `POST /mcp/message?sessionId=...` |
| 4 | Envia `notifications/initialized` | `POST /mcp/message?sessionId=...` |
| 5 | Envia `tools/list` | `POST /mcp/message?sessionId=...` |
| 6 | Envia `tools/call` | `POST /mcp/message?sessionId=...` |

### 1.2 Por que o EventFlux falha com o `SseEmitter`

O `McpController.java` atual escreve o evento `endpoint` manualmente usando `PrintWriter` com **LF apenas** (`\n`):

```java
writer.print("event: endpoint\n");
writer.print("data: " + endpointUrl + "\n\n");
```

Porém, a especificação SSE ([W3C](https://html.spec.whatwg.org/multipage/server-sent-events.html)) define que as linhas devem ser separadas por **CRLF** (`\r\n`), e os eventos terminados por **linha em branco CRLF** (`\r\n\r\n`).

O EventFlux (baseado no `flutter_client_sse`) faz parsing linha a linha com `RegExp` e split — quando recebe apenas `\n`, o buffer da Servlet API do Tomcat pode agregar/segmentar os bytes de forma que o parser do Dart interpreta o evento de forma incompleta, resultando em:

```
❌ EventFluxException: event is empty
❌ 等待endpoint确认超时 (timeout aguardando endpoint)
❌ message endpoint is not established
```

### 1.3 O que o teste deve provar

Um teste de integração automatizado deve validar que o servidor responde no formato que o EventFlux espera:

| Verificação | Esperado |
|-------------|----------|
| `Content-Type` | `text/event-stream; charset=utf-8` |
| `Cache-Control` | `no-cache` |
| `Connection` | `keep-alive` |
| `X-Accel-Buffering` | `no` |
| Linhas do evento | Separadas por `\r\n` (CRLF) |
| Fim do evento | `\r\n\r\n` (linha em branco CRLF) |
| Evento `endpoint` | `event: endpoint\r\ndata: /mcp/message?sessionId=...\r\n\r\n` |
| POST `/mcp/message` sem sessão | HTTP 404 |

---

## 2. Pré-requisitos

- [ ] Java 21+ (`java --version`)
- [ ] Maven (`mvn --version`)
- [ ] Projeto `configuracao-ddl-mcp` compilando
- [ ] Dependência `spring-boot-starter-test` no `pom.xml` (já existe)

```bash
cd C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp
mvn clean install -DskipTests
```

---

## 3. Passo a Passo — Criar o Teste de Integração

### Passo 3.1 — Criar a classe de teste

**Arquivo:** `configuracao-ddl-mcp/src/test/java/com/thadeu/massa_dados_core/config/McpSseIntegrationTest.java`

```java
package com.thadeu.massa_dados_core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thadeu.massa_dados_core.mcp.DdlToolsService;
import com.thadeu.massa_dados_core.mcp.dto.DdlResponse;
import com.thadeu.massa_dados_core.mcp.dto.UnknownEntityResponse;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração para o {@link McpController} com foco na compatibilidade
 * SSE com o cliente EventFlux (ChatMCP).
 *
 * <p>Valida o formato exato dos eventos SSE (CRLF), os headers HTTP do handshake
 * e o fluxo JSON-RPC completo (initialize, tools/list, tools/call).</p>
 *
 * @author Thadeu Garrido
 * @version 1.0
 */
@DisplayName("McpSseIntegrationTest")
class McpSseIntegrationTest {

    private static final String SSE_CRLF = "\r\n";

    private MockMvc mockMvc;
    private DdlToolsService ddlToolsService;

    @BeforeEach
    void setUp() {
        ddlToolsService = mock(DdlToolsService.class);
        McpController controller = new McpController(ddlToolsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ============================================================
    // 1. Handshake GET /mcp — formato SSE esperado pelo EventFlux
    // ============================================================

    @Test
    @DisplayName("deveRetornarHeadersSSECorretosNoHandshake — Content-Type, Cache-Control, Connection e X-Accel-Buffering")
    void deveRetornarHeadersSSECorretosNoHandshake() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/event-stream")))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(header().string("Connection", "keep-alive"))
                .andExpect(header().string("X-Accel-Buffering", "no"));
    }

    @Test
    @DisplayName("deveRetornarEventoEndpointSeparadoPorCRLF — linhas terminadas em \\r\\n e evento fechado com linha em branco")
    void deveRetornarEventoEndpointSeparadoPorCRLF() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult finalResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        String body = finalResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Evento endpoint completo no formato CRLF
        assertTrue(body.startsWith("event: endpoint" + SSE_CRLF),
                "Evento deve iniciar com 'event: endpoint\\r\\n'. Body: " + body);

        assertTrue(body.contains("data: " + SSE_CRLF) || body.contains("data: http"),
                "Evento deve conter linha 'data:' com a URL do endpoint. Body: " + body);

        // Deve terminar com linha em branco CRLF (delimitador de fim de evento)
        assertTrue(body.endsWith(SSE_CRLF + SSE_CRLF),
                "Evento deve terminar com \\r\\n\\r\\n. Body: " + body);

        // Não deve haver LF isolado (sem CR antes) — o EventFlux se perde com LF puro
        String semCrlf = body.replace(SSE_CRLF, "");
        assertFalse(semCrlf.contains("\n"),
                "Não deve haver LF isolado (sem CR) no body SSE. Body: " + body);
    }

    @Test
    @DisplayName("deveIncluirSessionIdNoEndpoint — URL do endpoint deve conter sessionId e caminho /mcp/message")
    void deveIncluirSessionIdNoEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult finalResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        String body = finalResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("/mcp/message?"),
                "Endpoint deve apontar para /mcp/message?sessionId=... Body: " + body);
        assertTrue(body.matches("(?s).*sessionId=[a-fA-F0-9-]{36}.*"),
                "Endpoint deve conter um sessionId UUID válido. Body: " + body);
    }

    // ============================================================
    // 2. POST /mcp/message — fluxo JSON-RPC via sessão SSE
    // ============================================================

    @Test
    @DisplayName("deveProcessarInitializeAtravesDaSessao — initialize deve retornar protocolVersion e serverInfo")
    void deveProcessarInitializeAtravesDaSessao() throws Exception {
        String sessionId = abreSessaoSse();

        MvcResult postResult = mockMvc.perform(post("/mcp/message")
                        .param("sessionId", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = postResult.getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(responseBody);

        assertEquals("2.0", json.path("jsonrpc").asText());
        assertEquals(1, json.path("id").asInt());
        assertEquals("2024-11-05", json.path("result").path("protocolVersion").asText());
        assertEquals("configuracao-ddl-mcp", json.path("result").path("serverInfo").path("name").asText());
    }

    @Test
    @DisplayName("deveListarFerramentasViaSessao — tools/list deve retornar as 3 ferramentas registradas")
    void deveListarFerramentasViaSessao() throws Exception {
        String sessionId = abreSessaoSse();

        MvcResult postResult = mockMvc.perform(post("/mcp/message")
                        .param("sessionId", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "method": "tools/list",
                                  "id": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = postResult.getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(responseBody);

        JsonNode tools = json.path("result").path("tools");
        assertTrue(tools.isArray());
        assertEquals(3, tools.size());

        assertTrue(tools.toString().contains("ddl_to_entity"));
        assertTrue(tools.toString().contains("identify_unknown_entities"));
        assertTrue(tools.toString().contains("get_generation_config"));
    }

    @Test
    @DisplayName("deveExecutarFerramentaDdlToEntity — tools/call deve invocar o serviço e retornar conteúdo")
    void deveExecutarFerramentaDdlToEntity() throws Exception {
        // Mock do serviço — não depende de compilação real
        when(ddlToolsService.ddlToEntity(anyString()))
                .thenReturn(new DdlResponse("Teste", "T696SSE",
                        "@Entity\npublic class Teste {}", true, "OK",
                        java.util.List.of(), java.util.List.of(), java.util.List.of()));

        String sessionId = abreSessaoSse();

        MvcResult postResult = mockMvc.perform(post("/mcp/message")
                        .param("sessionId", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = postResult.getResponse().getContentAsString();
        JsonNode json = new ObjectMapper().readTree(responseBody);

        assertTrue(json.path("result").path("content").isArray());
        assertTrue(json.path("result").path("content").toString().contains("T696SSE"));

        verify(ddlToolsService).ddlToEntity(contains("CREATE TABLE T696SSE"));
    }

    @Test
    @DisplayName("deveExecutarFerramentaIdentifyUnknownEntities — tools/call deve retornar tabelas desconhecidas")
    void deveExecutarFerramentaIdentifyUnknownEntities() throws Exception {
        when(ddlToolsService.identifyUnknownEntities(anyString()))
                .thenReturn(new UnknownEntityResponse(java.util.List.of("T700DESC"),
                        java.util.List.of(), java.util.Map.of()));

        String sessionId = abreSessaoSse();

        MvcResult postResult = mockMvc.perform(post("/mcp/message")
                        .param("sessionId", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = postResult.getResponse().getContentAsString();
        assertTrue(responseBody.contains("T700DESC"));

        verify(ddlToolsService).identifyUnknownEntities(anyString());
    }

    // ============================================================
    // 3. Tratamento de erros
    // ============================================================

    @Test
    @DisplayName("deveRetornar404ParaSessaoInexistente — POST /mcp/message com sessão inválida deve retornar 404")
    void deveRetornar404ParaSessaoInexistente() throws Exception {
        mockMvc.perform(post("/mcp/message")
                        .param("sessionId", "sessao-inexistente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deveProcessarNotificationInitialized — notificação não deve quebrar a sessão")
    void deveProcessarNotificationInitialized() throws Exception {
        String sessionId = abreSessaoSse();

        mockMvc.perform(post("/mcp/message")
                        .param("sessionId", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jsonrpc": "2.0",
                                  "method": "notifications/initialized",
                                  "params": {}
                                }
                                """))
                .andExpect(status().isOk());
    }

    // ============================================================
    // 4. POST /mcp — modo JSON síncrono (regressão)
    // ============================================================

    @Test
    @DisplayName("deveProcessarInitializacaoViaPostJson — POST /mcp sem Accept SSE deve responder JSON")
    void deveProcessarInitializacaoViaPostJson() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"))
                .andExpect(jsonPath("$.result.serverInfo.name").value("configuracao-ddl-mcp"));
    }

    // ============================================================
    // Métodos auxiliares
    // ============================================================

    /**
     * Abre uma sessão SSE via GET /mcp e extrai o sessionId do evento endpoint.
     *
     * @return sessionId UUID extraído do handshake SSE
     */
    private String abreSessaoSse() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult finalResult = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        String body = finalResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("sessionId=([a-fA-F0-9-]{36})").matcher(body);
        assertTrue(matcher.find(), "Endpoint deve conter sessionId. Body: " + body);
        return matcher.group(1);
    }
}
```

> ⚠️ **Nota:** Este teste usa `MockMvcBuilders.standaloneSetup` para testar o controller isoladamente, com `DdlToolsService` mockado — assim não depende de entidades JPA, H2 ou compilação Maven.

---

### Passo 3.2 — Executar o teste

```powershell
cd C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp
mvn test -Dtest=McpSseIntegrationTest
```

**Resultado esperado após a correção do controller:**

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Se falhar com `deveRetornarEventoEndpointSeparadoPorCRLF`:** o controller ainda usa `\n` — vá para o **Passo 4**.

---

## 4. Passo a Passo — Corrigir o `McpController` (trocar LF por CRLF)

**Arquivo:** `configuracao-ddl-mcp/src/main/java/com/thadeu/massa_dados_core/config/McpController.java`

Na classe ativa, o método `handleSseHandshake` escreve o evento `endpoint` com `\n`:

```java
// ❌ ANTES — LF apenas (quebra o EventFlux)
writer.print("event: endpoint\n");
writer.print("data: " + endpointUrl + "\n\n");
```

**Alterar para CRLF:**

```java
// ✅ DEPOIS — CRLF (conforme especificação SSE e esperado pelo EventFlux)
writer.print("event: endpoint\r\n");
writer.print("data: " + endpointUrl + "\r\n\r\n");
```

> 💡 **Por que CRLF?** O EventFlux (Dart) herda do `flutter_client_sse` e monta os eventos usando `RegExp(r'\r?\n\r?\n')` para separar eventos e depois quebra por linha para extrair `event:` e `data:`. Quando o buffer chega com `\n` puro, o parser pode interpretar o `data:` de um evento parcial como parte de outro, resultando em `EventFluxException` ou timeout do `endpoint`.

---

## 5. Passo a Passo — Validar a correção

### Passo 5.1 — Re-executar o teste de integração

```powershell
mvn test -Dtest=McpSseIntegrationTest
```

**Esperado:** `BUILD SUCCESS`

### Passo 5.2 — Executar todos os testes (regressão)

```powershell
mvn test
```

### Passo 5.3 — Validação manual com `curl` (bytes brutos)

Subir o servidor:

```powershell
mvn spring-boot:run
```

Em outro terminal:

```powershell
curl -N -v http://localhost:8081/mcp 2>&1 | Select-String "event:|data:|< HTTP"
```

**Esperado (bytes com `\r\n` visíveis em modo verbose):**

```
< HTTP/1.1 200
< Content-Type: text/event-stream;charset=utf-8
< Cache-Control: no-cache
< Connection: keep-alive
< X-Accel-Buffering: no
*
event: endpoint
data: http://localhost:8081/mcp/message?sessionId=...
*
```

> Para ver os bytes CRLF explicitamente, use:
> ```powershell
> curl -N -s http://localhost:8081/mcp | Format-Hex
> ```
> Os bytes `0D 0A` (CR LF) devem aparecer entre cada linha, e `0D 0A 0D 0A` ao final do evento.

### Passo 5.4 — Conferir se o GET /mcp ficou aberto (sem timeout)

O `curl -N` deve permanecer **pendurado** (conexão aberta), aguardando novos eventos — evidenciando que a conexão SSE está ativa.

---

## 6. Validação no ChatMCP (opcional — teste manual final)

1. Fechar e reabrir o ChatMcp.
2. No `mcp_server.json`, o bloco `configuracao-ddl-mcp` deve estar:
   ```json
   "configuracao-ddl-mcp": {
     "name": "configuracao-ddl-mcp",
     "type": "sse",
     "command": "http://localhost:8081/mcp",
     "args": [],
     "env": {},
     "auto_approve": true,
     "installed": true
   }
   ```
3. Ligar o toggle do servidor.
4. **Logs esperados sem erro:**
   ```
   INFO: 开始建立SSE连接: http://localhost:8081/mcp
   INFO: 建立SSE连接到: http://localhost:8081/mcp
   INFO: event: endpoint, data: http://localhost:8081/mcp/message?sessionId=...
   INFO: endpoint: http://localhost:8081/mcp/message?sessionId=...
   INFO: MCP server initialized successfully
   INFO: Tools registered: [ddl_to_entity, identify_unknown_entities, get_generation_config]
   ```
5. **Nenhum** `EventFluxException`, `等待endpoint确认超时` ou `message endpoint is not established`.

---

## 7. Execução da suíte completa (padrão do projeto)

Para rodar todos os testes do módulo:

```powershell
cd C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp
mvn clean install
``