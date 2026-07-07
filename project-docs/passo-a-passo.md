# 📋 Passo a Passo — Servidor MCP para Geração de Massa de Dados

## 🎯 Objetivo
Criar dois servidores MCP (Model Context Protocol) usando Spring Boot. O primeiro servidor:
- Recebe uma configuração via application.properties informando onde estão as classes entities
- Disponibiliza ao chat-mcp a funcionalidade de ao receber um script ddl, atualizar as classes entities do segundo servidor e recompilar;

O segundo servidor:
- Analisa a estrutura da entidade (reflection)
- Retorna um dicionário de metadados: nome da classe, atributos, tipos, anotações JPA, etc. Que possam responder a uma série de perguntas com o vocabulário de negócio. O que não conseguir resolver, gerar uma demanda para um técnico vir configurar estes metadados e o sistema evoluir.
- Permite que o cliente chat MCP receba comandos na linguagem do domínio da aplicação e crie a massa de dados. Exemplo: Crie um portfolio com duas ações estratégicas, ano do portfolio 2026, duas etapas em cada ação, com entregas nos meses setembro, outubro e desembro. Pelas entidades ele vai saber onde criar cada registro, mas para o usuário do chat (que é da área e negócio) vai só pedir a massa para a execução das homologações.

## 🧱 Arquitetura
```
Cliente MCP (Continue)
    ↓ (JSON-RPC via HTTP)
Servidor MCP 1 — Configuração e DDL (Spring Boot)
    ├── McpServerConfig (configuração do protocolo MCP)
    ├── McpToolHandler (ferramentas: "ddl_to_entity", "identify_unknown_entities")
    ├── McpDdlToEntityService (conversão de DDL para classes Entity)
    ├── McpUnknownEntityService (identificação de classes/atributos não reconhecidos)
    ├── McpCompileService (recompilação do Servidor 2)
    └── dto/ (DdlRequest, DdlResponse, UnknownEntityRequest, UnknownEntityResponse)
        ↓ (chamada interna para recompilar)
Servidor MCP 2 — Metadados e Massa de Dados (Spring Boot)
    ├── McpServerConfig (configuração do protocolo MCP)
    ├── McpToolHandler (ferramenta "get_entity_metadata")
    ├── McpEntityMetadataService (lógica de reflection)
    └── dto/ (EntityMetadataRequest, EntityMetadataResponse)
```

## ✅ Checklist

### Fase 1 — Configuração do Projeto
- [ ] 1.1 Adicionar dependências no `pom.xml`:
  - Spring Boot Starter Web
  - Spring Boot Starter Data JPA
  - Spring Boot Starter Validation
  - Lombok
  - PostgreSQL Driver (runtime)
  - Biblioteca MCP para Java (ex: `io.modelcontextprotocol:mcp-spring-boot-starter` ou implementação manual)
- [ ] 1.2 Criar `application.properties` com:
  - Porta do servidor (ex: 8080)
  - Configuração do datasource (opcional, se quiser consultar banco real)
- [ ] 1.3 Transformar `App.java` em `@SpringBootApplication`
- [ ] 1.4 Verificar se o `.gitignore` ignora `project-docs/`

### Fase 2 — Implementação do Servidor MCP
- [ ] 2.1 Criar pacote `mcp/` com:
  - `McpServerConfig.java` — configuração do protocolo MCP (transporte HTTP, registro de ferramentas)
  - `McpToolHandler.java` — implementação da ferramenta `get_entity_metadata`
  - `McpEntityMetadataService.java` — serviço que usa reflection para extrair metadados
- [ ] 2.2 Criar pacote `mcp/dto/` com:
  - `EntityMetadataRequest.java` — record com campo `className` (String)
  - `EntityMetadataResponse.java` — record com campos:
    - `className` (String)
    - `tableName` (String, da anotação `@Table`)
    - `attributes` (List<AttributeInfo>)
    - `relationships` (List<RelationshipInfo>)
- [ ] 2.3 Implementar `McpEntityMetadataService`:
  - Carregar classe pelo nome (`Class.forName`)
  - Verificar se tem `@Entity`
  - Extrair `@Table(name)`
  - Para cada campo declarado:
    - Nome, tipo, anotações JPA (`@Column`, `@Id`, `@GeneratedValue`, `@ManyToOne`, etc.)
    - Se for relacionamento, extrair a entidade alvo
  - Retornar `EntityMetadataResponse`

### Fase 3 — Testes
- [ ] 3.1 Testar com uma entidade concreta (ex: `Cooperado` da biblioteca `pessoa-core`)
- [ ] 3.2 Verificar resposta JSON no formato esperado pelo MCP
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
src/main/java/com/thadeu/massa-dados-core/
├── App.java                          (Spring Boot Application)
├── config/
│   ├── CucumberPicoFactory.java
│   └── InjectPageFactory.java
├── mcp/
│   ├── McpServerConfig.java
│   ├── McpToolHandler.java
│   ├── McpEntityMetadataService.java
│   ├── McpDdlToEntityService.java
│   ├── McpUnknownEntityService.java
│   └── dto/
│       ├── EntityMetadataRequest.java
│       ├── EntityMetadataResponse.java
│       ├── DdlRequest.java
│       ├── DdlResponse.java
│       ├── UnknownEntityRequest.java
│       └── UnknownEntityResponse.java
└── resources/
    └── application.properties
```

## 🔗 Referências
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Reflection API Java](https://docs.oracle.com/javase/tutorial/reflect/)
