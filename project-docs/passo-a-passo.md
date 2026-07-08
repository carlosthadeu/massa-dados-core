# 📋 Passo a Passo — Servidor MCP para Geração de Massa de Dados

## 🎯 Objetivo
Criar dois servidores MCP (Model Context Protocol) usando Spring Boot. O primeiro servidor (configuracao-ddl-mcp):
- Recebe uma configuração via application.properties informando onde estão as classes entities
- Disponibiliza ao chat-mcp a funcionalidade de ao receber um script ddl, atualizar as classes entities do segundo servidor e recompilar;

O segundo servidor (metadados-massa-mcp):
- Analisa a estrutura da entidade (reflection)
- Retorna um dicionário de metadados: nome da classe, atributos, tipos, anotações JPA, etc. Que possam responder a uma série de perguntas com o vocabulário de negócio. O que não conseguir resolver, gerar uma demanda para um técnico vir configurar estes metadados e o sistema evoluir.
- Permite que o cliente chat MCP receba comandos na linguagem do domínio da aplicação e crie a massa de dados. Exemplo: Crie um portfolio com duas ações estratégicas, ano do portfolio 2026, duas etapas em cada ação, com entregas nos meses setembro, outubro e desembro. Pelas entidades ele vai saber onde criar cada registro, mas para o usuário do chat (que é da área e negócio) vai só pedir a massa para a execução das homologações.

## 🧱 Arquitetura
```
Cliente MCP (Continue)
    ↓ (JSON-RPC via HTTP)
configuracao-ddl-mcp — Configuração e DDL (Spring Boot, porta 8081)
    ├── McpServerConfig (configuração do protocolo MCP)
    ├── McpToolHandler (ferramentas: "ddl_to_entity", "identify_unknown_entities")
    ├── McpDdlToEntityService (conversão de DDL para classes Entity)
    ├── McpUnknownEntityService (identificação de classes/atributos não reconhecidos)
    ├── McpCompileService (recompilação do Servidor 2 via ProcessBuilder)
    └── dto/ (DdlRequest, DdlResponse, UnknownEntityRequest, UnknownEntityResponse)
        ↓ (chamada interna para recompilar)
metadados-massa-mcp — Metadados e Massa de Dados (Spring Boot, porta 8082)
    ├── McpServerConfig (configuração do protocolo MCP)
    ├── McpToolHandler (ferramenta "get_entity_metadata")
    ├── McpEntityMetadataService (lógica de reflection)
    └── dto/ (EntityMetadataRequest, EntityMetadataResponse)
```

## ✅ Checklist

### Fase 1 — Configuração do Projeto
- [ ] 1.1 Criar dois projetos Maven separados na raiz:
  - `configuracao-ddl-mcp/` (porta 8081)
  - `metadados-massa-mcp/` (porta 8082)
- [ ] 1.2 Cada projeto terá seu próprio `pom.xml` com dependências:
  - Spring Boot Starter Web
  - Spring Boot Starter Data JPA
  - Spring Boot Starter Validation
  - Lombok
  - H2 Database (runtime)
  - Spring Boot Starter Test (test)
  - **Não** usar `mcp-spring-boot-starter` (não existe no Maven Central)
- [ ] 1.3 Cada projeto terá seu próprio `application.properties`:
  - Porta do servidor (8081 para Servidor 1, 8082 para Servidor 2)
  - Configuração do datasource H2
  - Caminho das classes Entity (`entity.classes.path`)
  - Caminho do projeto para recompilação (`entity.project.path`)
- [ ] 1.4 Cada projeto terá seu próprio `App.java` com `@SpringBootApplication`
- [ ] 1.5 Verificar se o `.gitignore` ignora `project-docs/`

### Fase 2 — Implementação dos Servidores MCP

#### configuracao-ddl-mcp — Configuração e DDL (porta 8081)
- [ ] 2.1 Criar pacote `mcp/` no Servidor 1 com:
  - `McpServerConfig.java` — configuração do protocolo MCP (endpoint `/mcp` com JSON-RPC)
  - `McpToolHandler.java` — implementação das ferramentas `ddl_to_entity` e `identify_unknown_entities`
  - `McpDdlToEntityService.java` — conversão de DDL para classes Entity
  - `McpUnknownEntityService.java` — identificação de classes/atributos não reconhecidos
  - `McpCompileService.java` — recompilação do Servidor 2 via `ProcessBuilder`
- [ ] 2.2 Criar pacote `mcp/dto/` no Servidor 1 com:
  - `DdlRequest.java` — record com campo `ddlScript` (String)
  - `DdlResponse.java` — record com campos:
    - `entityClassName` (String)
    - `entityCode` (String)
    - `tableName` (String)
    - `attributes` (List<AttributeInfo>)
  - `UnknownEntityRequest.java` — record com campo `ddlScript` (String)
  - `UnknownEntityResponse.java` — record com campos:
    - `missingTables` (List<String>)
    - `missingColumns` (List<MissingColumnInfo>)

#### metadados-massa-mcp — Metadados e Massa de Dados (porta 8082)
- [ ] 2.3 Criar pacote `mcp/` no Servidor 2 com:
  - `McpServerConfig.java` — configuração do protocolo MCP (endpoint `/mcp` com JSON-RPC)
  - `McpToolHandler.java` — implementação da ferramenta `get_entity_metadata`
  - `McpEntityMetadataService.java` — serviço que usa reflection para extrair metadados
- [ ] 2.4 Criar pacote `mcp/dto/` no Servidor 2 com:
  - `EntityMetadataRequest.java` — record com campo `className` (String)
  - `EntityMetadataResponse.java` — record com campos:
    - `className` (String)
    - `tableName` (String, da anotação `@Table`)
    - `attributes` (List<AttributeInfo>)
    - `relationships` (List<RelationshipInfo>)
- [ ] 2.5 Implementar `McpEntityMetadataService`:
  - Carregar classe pelo nome (`Class.forName`)
  - Verificar se tem `@Entity`
  - Extrair `@Table(name)`
  - Para cada campo declarado:
    - Nome, tipo, anotações JPA (`@Column`, `@Id`, `@GeneratedValue`, `@ManyToOne`, etc.)
    - Se for relacionamento, extrair a entidade alvo
  - Retornar `EntityMetadataResponse`

### Fase 3 — Testes
- [ ] 3.1 Testar com as entidades em C:\Desenvolvimento\massa-dados-core\project-docs\dominio-aplicacao
      3.1.1 Criar um package que seja compartilhado entre os dois servidores mcp
        3.1.1.1 O configuracao-ddl-mcp irá fazer a atualização entre o ddl e as entities
        3.1.1.2 O metadados-massa-mcp irá utilizar estas classes para acessar a base de dados para CRUD, de acordo com o solicitado através do chat-mcp
        3.1.1.3 As entities contidas em C:\Desenvolvimento\massa-dados-core\project-docs\dominio-aplicacao não devem ser corrigidas pelo aider. Somente pelo configuracao-ddl-mcp
- [ ] 3.2 Verificar respostas no chat-mcp com os dois servidores configurados
- [ ] 3.3 Testar erro para classe inexistente ou sem `@Entity`

### Fase 4 — Integração com Continue
- [ ] 4.1 Configurar o servidor MCP no `~/.continue/config.json` (ou `config.ts`)
- [ ] 4.2 Testar chamada da ferramenta `get_entity_metadata` pelo Continue
- [ ] 4.3 Validar que o dicionário retornado permite gerar massa de dados

### Fase 5 — Funcionalidades Avançadas
- [ ] 5.1 **Ferramenta `ddl_to_entity`**:
  - Receber um script DDL (CREATE TABLE, ALTER TABLE, etc.)
  - Analisar sintaxe SQL e extrair:
    - Nome da tabela
    - Colunas (nome, tipo, nullable, primary key, foreign key)
  - Gerar código Java da classe Entity correspondente (com anotações JPA)
  - Salvar o arquivo no diretório `entity/` do Servidor 2
  - Após salvar, chamar recompilação do Servidor 2 (via `McpCompileService`)
  - Atualizar o dicionário de metadados
- [ ] 5.2 **Ferramenta `identify_unknown_entities`**:
  - Receber um script DDL
  - Comparar tabelas/colunas do DDL com as classes Entity existentes no projeto
  - Identificar:
    - Tabelas que não possuem classe Entity correspondente
    - Colunas que não possuem atributo correspondente na Entity
  - Retornar lista de itens não reconhecidos
- [ ] 5.3 **Integração entre ferramentas**:
  - Após `identify_unknown_entities`, o usuário pode chamar `ddl_to_entity` para gerar as classes faltantes
  - O dicionário deve ser atualizado automaticamente após cada geração
- [ ] 5.4 **Recompilação do Servidor 2**:
  - Criar `McpCompileService` no Servidor 1 que:
    - Executa `mvn compile` no diretório do Servidor 2 (via `ProcessBuilder`)
    - Ou chama um endpoint REST no Servidor 2 para recarregar classes (ex: `/actuator/restart`)
    - Retorna sucesso/erro da compilação
  - Integrar `McpCompileService` no fluxo de `ddl_to_entity`
  - Garantir que o Servidor 2 esteja configurado para aceitar recarga (ex: Spring Boot DevTools ou actuator restart)

## 📦 Estrutura de Diretórios (após implementação)
```
raiz-do-repositorio/
├── configuracao-ddl-mcp/          (porta 8081)
│   ├── pom.xml
│   └── src/main/java/com/thadeu/massa_dados_core/
│       ├── App.java
│       ├── mcp/
│       │   ├── McpServerConfig.java
│       │   ├── McpToolHandler.java
│       │   ├── McpDdlToEntityService.java
│       │   ├── McpUnknownEntityService.java
│       │   ├── McpCompileService.java
│       │   └── dto/
│       │       ├── DdlRequest.java
│       │       ├── DdlResponse.java
│       │       ├── UnknownEntityRequest.java
│       │       └── UnknownEntityResponse.java
│       └── resources/
│           └── application.properties
│
├── metadados-massa-mcp/           (porta 8082)
│   ├── pom.xml
│   └── src/main/java/com/thadeu/massa_dados_core/
│       ├── App.java
│       ├── mcp/
│       │   ├── McpServerConfig.java
│       │   ├── McpToolHandler.java
│       │   ├── McpEntityMetadataService.java
│       │   └── dto/
│       │       ├── EntityMetadataRequest.java
│       │       └── EntityMetadataResponse.java
│       └── resources/
│           └── application.properties
│
├── src/                                (projeto original, será removido)
├── pom.xml                             (projeto original, será removido)
├── README.md
├── USAGE.md
└── project-docs/
    └── passo-a-passo.md
```

## 🔗 Referências
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Reflection API Java](https://docs.oracle.com/javase/tutorial/reflect/)
