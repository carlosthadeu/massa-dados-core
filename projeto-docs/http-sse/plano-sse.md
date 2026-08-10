# 🔌 Plano: Implementar acesso via SSE no `configuracao-ddl-mcp` para o ChatMcp

> **Data:** 2026-08-06
> **Servidor alvo:** `configuracao-ddl-mcp` (porta 8081)
> **Objetivo:** Habilitar o transporte **SSE (Server-Sent Events)** no servidor Java e configurar o ChatMcp para consumi-lo via `http://localhost:8081/sse`.

---

## 1. Contexto

### 1.1 O que é o transporte SSE no MCP?

O transporte **SSE** é o protocolo MCP legado (protocolo `2024-11-05`) que usa:

| Método | Endpoint | Função |
|--------|----------|--------|
| `GET` | `/sse` | Abre o stream de eventos SSE |
| `POST` | `/message` | Envia mensagens JSON-RPC ao servidor (a URL vem no evento `endpoint`) |

O servidor envia primeiro um evento `endpoint` no stream SSE:

```text
event: endpoint
data: /message
```

O cliente então usa `POST /message` para enviar JSON-RPC e recebe as respostas pelo stream SSE.

### 1.2 Estado atual

O `configuracao-ddl-mcp` **não** expõe o transporte SSE — apenas o **Streamable HTTP** (`POST /mcp`) via `spring-ai-starter-mcp-server-webmvc`.

Por isso o ChatMcp (configurado com `type: "sse"`) falha com `EventFluxException` e timeout do `endpoint`.

### 1.3 Objetivo

Adicionar o starter `spring-ai-starter-mcp-server-webmvc-sse` ao projeto para expor **simultaneamente** os dois transportes:

| Transporte | Endpoint | Starter |
|------------|----------|---------|
| Streamable HTTP | `POST /mcp` | `spring-ai-starter-mcp-server-webmvc` (já existente) |
| SSE | `GET /sse` + `POST /message` | `spring-ai-starter-mcp-server-webmvc-sse` (a adicionar) |

---

## 2. Pré-requisitos

- [ ] Java 21+ instalado (`java --version`)
- [ ] Maven instalado (`mvn --version`)
- [ ] Projeto `configuracao-ddl-mcp` compilando (execute `mvn clean install -DskipTests` antes)
- [ ] Acesso ao arquivo `pom.xml` do servidor

---

## 3. Passo a passo

### Passo 1 — Adicionar a dependência SSE no `pom.xml`

Editar o arquivo:

```
C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp\pom.xml
```

Adicionar a dependência `spring-ai-starter-mcp-server-webmvc-sse` **logo após** a dependência `spring-ai-starter-mcp-server-webmvc` existente:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    <version>${spring-ai.version}</version>
</dependency>

<!-- NOVO: Habilita o transporte SSE (GET /sse + POST /message) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc-sse</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

> **Nota:** O `<version>` pode ser omitido se o BOM (`spring-ai-bom`) já gerenciar a versão — mas no `pom.xml` atual as dependências do Spring AI declaram `<version>` explicitamente, então mantenha o padrão.

---

### Passo 2 — (Opcional) Configurar nome e parâmetros do servidor SSE

No arquivo `application.properties`:

```
C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp\src\main\resources\application.properties
```

Adicionar (opcional, somente se quiser personalizar):

```properties
# Nome do servidor MCP (aparece no clientInfo)
spring.ai.mcp.server.name=configuracao-ddl-mcp

# Versão do protocolo MCP suportada
spring.ai.mcp.server.version=2024-11-05

# Capacidades (todas ativadas por padrão)
spring.ai.mcp.server.capabilities.tool=true
spring.ai.mcp.server.capabilities.resource=false
spring.ai.mcp.server.capabilities.prompt=false
spring.ai.mcp.server.capabilities.completion=false
```

> 💡 **Recomendação:** Manter apenas `tool=true` para expor somente as ferramentas (`ddl_to_entity`, `identify_unknown_entities`, `get_generation_config`) e reduzir o overhead.

---

### Passo 3 — Recompilar o servidor

No PowerShell, na raiz do projeto:

```powershell
cd C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp
mvn clean install -DskipTests
```

---

### Passo 4 — Subir o servidor

```powershell
cd C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp
mvn spring-boot:run
```

**Verificar nos logs** que o endpoint SSE foi registrado. Deve aparecer algo como:

```text
INFO o.e.j.server.handler.ContextHandler - Started o.e.j.s.ServletContextHandler@...{/sse,null,AVAILABLE}
INFO ... Started App in X.XXX seconds
```

---

### Passo 5 — Validar o endpoint SSE com curl

Com o servidor rodando, abra **outro terminal** e execute:

```powershell
curl -N http://localhost:8081/sse
```

**Resposta esperada (fica pendurado, mostrando o evento endpoint):**

```text
event: endpoint
data: /message

```

> Se aparecer isso, o SSE está **funcionando**.
> 
> Se o curl fechar imediatamente ou retornar erro, o transporte SSE não foi registrado — verificar se o starter foi adicionado corretamente e se o servidor subiu sem erros.

---

### Passo 6 — Configurar o ChatMcp para usar SSE

Editar o arquivo:

```
C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json
```

O bloco `configuracao-ddl-mcp` deve ficar **exatamente** assim:

```json
"configuracao-ddl-mcp": {
  "name": "configuracao-ddl-mcp",
  "type": "sse",
  "command": "http://localhost:8081/sse",
  "args": [],
  "env": {},
  "auto_approve": true,
  "installed": true
}
```

> O ChatMcp (`sse_client.dart`) ao conectar em `/sse`:
> 1. Abre o stream GET `/sse`;
> 2. Recebe o evento `endpoint` com `data: /message`;
> 3. Usa `POST /message` para enviar JSON-RPC;
> 4. Recebe as respostas pelo stream.

---

### Passo 7 — Reiniciar o ChatMcp e ativar o servidor

1. Fechar completamente o ChatMcp.
2. Reabrir.
3. Na seção de servidores MCP, ligar o toggle de `configuracao-ddl-mcp`.

---

### Passo 8 — Validar nos logs do ChatMcp

Esperar que **não** apareçam:

- ❌ `EventFluxException`
- ❌ `等待endpoint确认超时` (timeout aguardando endpoint)
- ❌ `message endpoint is not established`

E sim algo como:

```text
INFO: package:chatmcp/mcp/sse/sse_client.dart:88: 开始建立SSE连接: http://localhost:8081/sse
INFO: package:chatmcp/mcp/sse/sse_client.dart:99: 建立SSE连接到: http://localhost:8081/sse
INFO: endpoint received: /message
INFO: MCP server initialized successfully
```

---

## 4. Teste funcional

1. No ChatMcp, enviar a mensagem: *"Identifique entidades desconhecidas para: CREATE TABLE T696NOVA (SQ_NOVA INT NOT NULL PRIMARY KEY, NM_NOVA VARCHAR(100));"*
2. Se as ferramentas estiverem registradas, o ChatMcp invocará `identify_unknown_entities` via `POST /message`.

---

## 5. Validar também o HTTP (regressão)

Como o starter `spring-ai-starter-mcp-server-webmvc` continua no `pom.xml`, o endpoint `POST /mcp` (Streamable HTTP) deve continuar funcionando:

```powershell
curl -X POST http://localhost:8081/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"curl\",\"version\":\"1.0\"}},\"id\":1}"
```

---

## 6. Rollback

Se quiser remover o SSE e manter apenas o HTTP:

1. Remover a dependência `spring-ai-starter-mcp-server-webmvc-sse` do `pom.xml`.
2. Recompilar: `mvn clean install -DskipTests`.
3. Reiniciar o servidor.

O servidor volta a expor apenas `POST /mcp`.

---

## 7. Resumo de alterações

| Arquivo | Alteração |
|---------|-----------|
| `configuracao-ddl-mcp/pom.xml` | Adicionar `spring-ai-starter-mcp-server-webmvc-sse` |
| `configuracao-ddl-mcp/src/main/resources/application.properties` | Adicionar `spring.ai.mcp.server.*` (opcional) |
| `C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json` | Manter `type: "sse"` e `command: "http://localhost:8081/sse"` (já está correto para SSE) |

---

## 8. Referências

- [Spring AI MCP Server Boot Starter — SSE](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stdio-sse-server-boot-starter-docs.html)
- [Model Context Protocol — SSE Transport](https://modelcontextprotocol.io/specification/2024-11-05/basic/transports)
- [ChatMcp — sse_client.dart](https://github.com/daodao97/chatmcp/blob/main/lib/mcp/sse/sse_client.dart)