# 📋 Passo a Passo — Servidor MCP para Geração de Massa de Dados

## 🎯 Objetivo
Criar um servidor MCP (Model Context Protocol) usando Spring Boot que:
- Recebe uma entidade JPA (Spring Data) via ferramenta MCP
- Analisa a estrutura da entidade (reflection)
- Retorna um dicionário de metadados: nome da classe, atributos, tipos, anotações JPA, etc.
- Permite que o cliente MCP (ex: Continue) entenda a estrutura da entidade para gerar massa de dados

## 🧱 Arquitetura
```
Cliente MCP (Continue)
    ↓ (JSON-RPC via HTTP)
Servidor MCP (Spring Boot)
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
│   └── dto/
│       ├── EntityMetadataRequest.java
│       └── EntityMetadataResponse.java
├── persist/
│   ├── generic/
│   │   ├── GenericDaoCommand.java
│   │   ├── GenericDaoList.java
│   │   ├── GenericDaoNamedQuery.java
│   │   ├── GenericDaoPu.java
│   │   ├── GenericDaoQueryParametros.java
│   │   ├── GenericDaoUnico.java
│   │   ├── GenericDaoUnmodifiableList.java
│   │   └── factories/
│   │       ├── DaoListFactory.java
│   │       ├── DaoQueryFactory.java
│   │       └── ParametrosDao.java
│   └── interfaces/
│       ├── DaoCommand.java
│       ├── DaoList.java
│       ├── DaoQuery.java
│       └── DaoUnique.java
└── resources/
    └── application.properties
```

## 🔗 Referências
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Reflection API Java](https://docs.oracle.com/javase/tutorial/reflect/)
