# 📡 Plano: Configurar ChatMcp para acessar `configuracao-ddl-mcp` via Streamable HTTP

> **Data:** 2026-08-06
> **Servidor alvo:** `configuracao-ddl-mcp` (porta 8081)
> **Objetivo:** Fazer o ChatMcp conectar ao servidor usando o transporte **Streamable HTTP** (`POST /mcp`), que é o que o `spring-ai-starter-mcp-server-webmvc` expõe por padrão.

---

## 1. Contexto

### 1.1 O que é Streamable HTTP?

O **Streamable HTTP** é o transporte MCP mais moderno (protocolo `2025-03-26` / `2024-11-05`). O cliente envia requisições `POST` JSON-RPC para um único endpoint (`/mcp`) e recebe respostas:
- **Síncronas:** retorno HTTP normal;
- **Assíncronas (streaming):** resposta com `Content-Type: application/json` e corpo contendo `SSE`-like, mas sobre POST.

### 1.2 Como o ChatMcp ativa o Streamable HTTP

No arquivo `mcp_server.json`, o bloco do servidor deve usar:

```json
{
  "type": "http",
  "command": "http://localhost:8081/mcp"
}
```

### 1.3 O que o servidor já expõe

O `configuracao-ddl-mcp` usa `spring-ai-starter-mcp-server-webmvc` (Spring AI **1.1.0**), que automaticamente expõe:

| Método | Endpoint | Protocolo |
|--------|----------|-----------|
| `POST` | `/mcp` | Streamable HTTP (JSON-RPC) |

> ⚠️ O servidor **NÃO** expõe `GET /sse` — por isso a configuração atual (`type: "sse"`, `command: ".../sse"`) falha com `EventFluxException`.

> ✅ **Log do servidor confirma:** já houve conexão bem-sucedida de um cliente Dart (nome `DartMCPClient`) via `/mcp`:
> ```
> 2026-07-13 12:31:33.652 INFO i.m.server.McpAsyncServer
> Client initialize request - Protocol: 2024-11-05, Capabilities: ...
> Info: Implementation[name=DartMCPClient, title=null, version=1.0.0]
> ```

---

## 2. Pré-requisitos

- [ ] Servidor `configuracao-ddl-mcp` compilado e rodando na porta `8081`
  ```powershell
  cd C:\Desenvolvimento\massa-dados-core\configuracao-ddl-mcp
  mvn spring-boot:run
  ```
- [ ] Porta 8081 respondendo:
  ```powershell
  curl -X POST http://localhost:8081/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"teste\",\"version\":\"1.0\"}},\"id\":1}"
  ```

---

## 3. Passo a passo

### Passo 1 — Localizar o arquivo de configuração do ChatMcp

O arquivo está em:

```
C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json
```

> No Windows, o caminho completo é:
> `C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json`

### Passo 2 — Fazer backup do arquivo atual

```powershell
Copy-Item "C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json" "C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.backup.json"
```

### Passo 3 — Editar o bloco `configuracao-ddl-mcp`

**Antes (configuração incorreta — SSE):**
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

**Depois (configuração correta — Streamable HTTP):**
```json
"configuracao-ddl-mcp": {
  "name": "configuracao-ddl-mcp",
  "type": "http",
  "command": "http://localhost:8081/mcp",
  "args": [],
  "env": {},
  "auto_approve": true,
  "installed": true
}
```

### Passo 4 — Salvar o arquivo

Garantir que o JSON fique **válido** (sem vírgulas sobrando, com chaves balanceadas).

### Passo 5 — Reiniciar o ChatMcp

- Fechar completamente o aplicativo ChatMcp.
- Abrir novamente.
- O ChatMcp lê `mcp_server.json` na inicialização.

### Passo 6 — Ativar o servidor MCP

Na interface do ChatMcp:
1. Abrir a seção de servidores MCP.
2. Encontrar `configuracao-ddl-mcp`.
3. Alternar o toggle para **ligado/on**.

### Passo 7 — Validar

Nos logs do ChatMcp, esperar algo como:

```text
INFO: package:chatmcp/mcp/mcp.dart:77: initializeMcpServer...
INFO: Using Streamable HTTP transport: http://localhost:8081/mcp
INFO: Tools registered: [ddl_to_entity, identify_unknown_entities, get_generation_config]
```

> Sem `EventFluxException`, sem `SSE连接错误`, sem timeout de `endpoint`.

---

## 4. Validação com curl (opcional, antes do ChatMcp)

Para confirmar que o endpoint `/mcp` está acessível:

```powershell
curl -X POST http://localhost:8081/mcp ^
  -H "Content-Type: application/json" ^
  -d '{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"curl\",\"version\":\"1.0\"}},\"id\":1}'
```

**Resposta esperada:** JSON-RPC com `result.protocolVersion`, `result.serverInfo`, `result.capabilities.tools` com as 3 ferramentas.

---

## 5. Teste funcional no ChatMcp

1. Enviar uma mensagem pedindo: *"Identifique entidades desconhecidas no DDL: CREATE TABLE T696FORU (SQ_FRU INT NOT NULL PRIMARY KEY, NM_FRU VARCHAR(100));"*
2. Se as ferramentas estiverem registradas, o ChatMcp listará `identify_unknown_entities` disponível.

---

## 6. Rollback

Se algo der errado, restaurar o backup:

```powershell
Copy-Item "C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.backup.json" "C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json" -Force
```

---

## 7. Resumo de alterações

| Arquivo | Alteração |
|---------|-----------|
| `C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json` | `type`: `"sse"` → `"http"` |
| `C:\Users\Foton\AppData\Roaming\ChatMcp\mcp_server.json` | `command`: `"http://localhost:8081/sse"` → `"http://localhost:8081/mcp"` |

**Nenhuma alteração no código Java é necessária** para o transporte HTTP — o servidor já o suporta nativamente.