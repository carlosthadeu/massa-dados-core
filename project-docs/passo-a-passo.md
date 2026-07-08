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
    ↓ (JSON-RPC via HTTP, endpoint /mcp)
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

### 📡 Protocolo JSON-RPC

Cada servidor expõe um endpoint `POST /mcp` que aceita o formato JSON-RPC 2.0:

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "get_entity_metadata",
    "arguments": {
      "className": "br.gov.bnb.domain.entity.Portfolio"
    }
  },
  "id": 1
}
```

Resposta de sucesso:
```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "{ \"className\": \"Portfolio\", \"tableName\": \"T696POAC\", ... }"
      }
    ]
  },
  "id": 1
}
```

Resposta de erro:
```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32603,
    "message": "Classe não encontrada: br.gov.bnb.domain.entity.PortfolioInexistente"
  },
  "id": 1
}
```

### 🧩 Records compartilhados

#### AttributeInfo
```java
public record AttributeInfo(
    String name,
    String type,
    boolean nullable,
    boolean isId,
    boolean isGeneratedValue,
    String columnName,
    Integer length,
    Integer precision,
    Integer scale,
    List<String> annotations
) {}
```

#### RelationshipInfo
```java
public record RelationshipInfo(
    String fieldName,
    String targetEntity,
    String mappedBy,
    String joinColumn,
    String relationshipType  // "ManyToOne", "OneToMany", "OneToOne", "ManyToMany"
) {}
```

#### MissingColumnInfo
```java
public record MissingColumnInfo(
    String tableName,
    String columnName,
    String columnType,
    boolean nullable
) {}
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
  - Configuração do datasource H2 (para teste e desenvolvimento)
  - Caminho das classes Entity (`entity.classes.path`) — caminho absoluto ou relativo para o diretório onde estão as classes `.java` das entities
  - Caminho do projeto para recompilação (`entity.project.path`) — caminho absoluto ou relativo para o diretório raiz do Servidor 2 (onde está o `pom.xml`)
  - Configuração do banco de dados real (SQL Server) para produção, via perfis Spring (`application-prod.properties`)
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

#### Configuração de CORS
- [ ] 2.6 Adicionar configuração CORS em ambos os servidores para permitir requisições de qualquer origem (necessário para o Continue chamar via HTTP)

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

#### Configuração de scan de pacotes
- [ ] 2.7 Configurar `@SpringBootApplication` para escanear também o pacote das entities:

```java
@SpringBootApplication(scanBasePackages = {
    "com.thadeu.massa_dados_core",
    "br.gov.bnb.domain.entity"
})
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```

#### Tratamento de erros padronizado
- [ ] 2.8 Implementar um `@ControllerAdvice` global em cada servidor para capturar exceções e retornar respostas JSON-RPC de erro padronizadas:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "jsonrpc", "2.0",
            "error", Map.of("code", -32603, "message", ex.getMessage()),
            "id", null
        ));
    }
}
```

### Fase 3 — Testes
- [ ] 3.1 Testar com as entidades em C:\Desenvolvimento\massa-dados-core\project-docs\dominio-aplicacao
      3.1.1 Criar um package que seja compartilhado entre os dois servidores mcp
        3.1.1.1 O configuracao-ddl-mcp irá fazer a atualização entre o ddl e as entities
        3.1.1.2 O metadados-massa-mcp irá utilizar estas classes para acessar a base de dados para CRUD, de acordo com o solicitado através do chat-mcp
        3.1.1.3 As entities contidas em C:\Desenvolvimento\massa-dados-core\project-docs\dominio-aplicacao não devem ser corrigidas pelo aider. Somente pelo configuracao-ddl-mcp
      3.1.2 Mecanismo de compartilhamento: ambos os servidores terão o mesmo diretório de entities como dependência local (via `pom.xml` com `<scope>compile</scope>` apontando para o caminho absoluto ou relativo). Alternativamente, usar um módulo Maven separado `entities-core` que ambos dependem.
- [ ] 3.2 Verificar respostas no chat-mcp com os dois servidores configurados
- [ ] 3.3 Testar classe inexistente — deve gerar a classe automaticamente (não é erro)
- [ ] 3.4 Testar DDL inválido (sintaxe SQL incorreta) — deve retornar erro JSON-RPC com código `-32602` (Invalid params)
- [ ] 3.5 Testar falha de compilação — `McpCompileService` deve retornar erro com detalhes da saída do Maven
- [ ] 3.6 Testar timeout na compilação — configurar timeout de 60 segundos no `ProcessBuilder`

### Fase 4 — Integração com Continue
- [ ] 4.1 Configurar o servidor MCP no `~/.continue/config.json` (ou `config.ts`)
- [ ] 4.2 Testar chamada da ferramenta `get_entity_metadata` pelo Continue
- [ ] 4.3 Validar que o dicionário retornado permite gerar massa de dados

### Fase 4.5 — Mapeamento Semântico (JSON + LLM)
- [ ] 4.5.1 Criar arquivo `mapeamento-semantico.json` com o mapeamento de termos de negócio para tabelas, colunas e valores (conforme exemplo na seção "Abordagem Híbrida")
- [ ] 4.5.2 O Servidor 2 deve carregar este JSON e disponibilizá-lo como contexto para o LLM
- [ ] 4.5.3 O LLM (Continue) usa o JSON para interpretar comandos em linguagem natural e gerar JSON estruturado de criação de massa
- [ ] 4.5.4 O Servidor 2 recebe o JSON estruturado e persiste os dados via JPA

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
    - Timeout de 60 segundos
    - Captura stdout e stderr para diagnóstico
    - Retorna sucesso/erro da compilação com detalhes
  - Integrar `McpCompileService` no fluxo de `ddl_to_entity`
  - Garantir que o Servidor 2 esteja configurado para aceitar recarga (ex: Spring Boot DevTools ou actuator restart)
  - Após compilação bem-sucedida, chamar endpoint `/actuator/restart` do Servidor 2 (se disponível) ou reiniciar o processo manualmente

#### Detalhamento do `McpCompileService`

```java
@Service
public class McpCompileService {

    @Value("${entity.project.path}")
    private String projectPath;

    public CompileResult compile() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "mvn.cmd", "compile", "-q"
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CompileResult(false, "Timeout após 60 segundos");
            }
            
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();
            
            return new CompileResult(exitCode == 0, output);
        } catch (Exception e) {
            return new CompileResult(false, e.getMessage());
        }
    }
    
    public record CompileResult(boolean success, String message) {}
}
```

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

### Abordagem Híbrida para Interpretação de Comandos (JSON + LLM)

**Como funciona:**

1. **JSON de Mapeamento Semântico** — Um arquivo JSON estático que mapeia termos de negócio para tabelas, colunas e valores numéricos. Exemplo:

```json
{
  "entidades": {
    "acao_estrategica": {
      "tabela": "T696ACES",
      "classe": "br.gov.bnb.domain.entity.AcaoEstrategica",
      "atributos": {
        "situacao": {
          "coluna": "ST_ACO_ETT",
          "mapeamento": {
            "Proposta em Edição": 3,
            "Ativo": 2,
            "Inativo": 1,
            "Proposta em Análise - Unidade": 4,
            "Proposta Rejeitada pela Unidade": 5,
            "Proposta em Planejamento": 6,
            "Proposta em Análise - Gestor de Unidade": 7,
            "Proposta em Análise - Amb. de Planejamento": 8,
            "Proposta em Análise - Superintendência": 9,
            "Proposta em Análise - Diretoria Executiva": 10
          }
        },
        "portfolio": {
          "coluna": "SQ_PTF_ACO",
          "relacionamento": "Portfolio"
        },
        "origem": {
          "coluna": "SQ_ORI_ACO",
          "relacionamento": "OrigemAcaoEstrategica"
        },
        "unidade_operacional": {
          "coluna": "CD_UND",
          "relacionamento": "UnidadeOperacional"
        },
        "unidade_aprovacao": {
          "coluna": "CD_UND_APR",
          "relacionamento": "UnidadeOperacional"
        },
        "data_limite": {
          "coluna": "DH_LIM",
          "tipo": "LocalDate"
        },
        "nome": {
          "coluna": "NM_ACO_ETT",
          "tipo": "String"
        },
        "descricao": {
          "coluna": "DE_ACO_ETT",
          "tipo": "String"
        }
      },
      "relacionamentos": {
        "etapas": {
          "tabela": "T696ETAC",
          "classe": "br.gov.bnb.domain.entity.EtapaAcaoEstrategica",
          "fk": "SQ_ACO_ETT"
        },
        "vinculacoes": {
          "tabela": "T696VIAC",
          "classe": "br.gov.bnb.domain.entity.VinculacaoAcaoEstrategica",
          "fk": "SQ_ACO_ETT"
        }
      }
    },
    "portfolio": {
      "tabela": "T696POAC",
      "classe": "br.gov.bnb.domain.entity.Portfolio",
      "atributos": {
        "nome": { "coluna": "NM_PTF", "tipo": "String" },
        "ano_realizacao": { "coluna": "AA_REA", "tipo": "Integer" },
        "situacao": {
          "coluna": "ST_PTF_ACO",
          "mapeamento": {
            "Em Elaboração": 1,
            "Em Execução": 2,
            "Em Replanejamento": 3,
            "Em Encerramento": 4,
            "Encerrado": 5
          }
        }
      }
    },
    "etapa_acao_estrategica": {
      "tabela": "T696ETAC",
      "classe": "br.gov.bnb.domain.entity.EtapaAcaoEstrategica",
      "atributos": {
        "descricao": { "coluna": "DE_ETP_ACO", "tipo": "String" },
        "entrega": { "coluna": "DE_ENT_ETP_ACO", "tipo": "String" },
        "ano_realizacao": { "coluna": "AA_REA", "tipo": "Integer" },
        "situacao": {
          "coluna": "ST_ETP_ACO",
          "mapeamento": {
            "Não iniciada": 1,
            "Em andamento": 2,
            "Atrasada": 3,
            "Não Concluída": 4,
            "Concluída com Atraso": 5,
            "Concluída": 6
          }
        }
      },
      "relacionamentos": {
        "entregas": {
          "tabela": "T696ENAC",
          "classe": "br.gov.bnb.domain.entity.Entrega",
          "fk": "SQ_ETP_ACO"
        }
      }
    },
    "entrega": {
      "tabela": "T696ENAC",
      "classe": "br.gov.bnb.domain.entity.Entrega",
      "atributos": {
        "mes_vigencia": { "coluna": "MM_ENT", "tipo": "Integer" },
        "planejamento": { "coluna": "PC_PLJ_ENT", "tipo": "BigDecimal" },
        "realizado": { "coluna": "PC_REA_ENT", "tipo": "BigDecimal" },
        "descricao_realizado": { "coluna": "DE_REA_ENT", "tipo": "String" },
        "situacao": {
          "coluna": "ST_ENT_ACO",
          "mapeamento": {
            "Não iniciada": 1,
            "Em andamento": 2,
            "Atrasada": 3,
            "Não Concluída": 4,
            "Concluída com Atraso": 5,
            "Concluída": 6
          }
        }
      }
    }
  }
}
```

2. **LLM com Function Calling** — O LLM recebe esse JSON como contexto e usa tools/functions para gerar comandos estruturados:

```json
{
  "comando": "criar_massa",
  "entidades": [
    {
      "tipo": "portfolio",
      "dados": {
        "nome": "Portfolio Homologação 2026",
        "ano_realizacao": 2026,
        "situacao": "Em Elaboração"
      },
      "filhos": [
        {
          "tipo": "acao_estrategica",
          "dados": {
            "nome": "Ação 1",
            "descricao": "Descrição da ação 1",
            "situacao": "Proposta em Edição"
          },
          "filhos": [
            {
              "tipo": "etapa_acao_estrategica",
              "dados": {
                "descricao": "Etapa 1",
                "entrega": "Entrega 1",
                "ano_realizacao": 2026,
                "situacao": "Não iniciada"
              },
              "filhos": [
                {
                  "tipo": "entrega",
                  "dados": {
                    "mes_vigencia": 9,
                    "planejamento": 50.00,
                    "situacao": "Não iniciada"
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

3. **Servidor 2 executa** — O JSON estruturado é enviado para o Servidor 2 que usa JPA para persistir os dados no banco.

**Por que essa abordagem?**
- ✅ **Simplicidade:** JSON é fácil de criar e manter
- ✅ **Flexibilidade:** O LLM lida com variações linguísticas naturalmente
- ✅ **Controle:** O JSON define exatamente quais valores são válidos
- ✅ **Evolução:** Adicionar novas entidades = adicionar novo bloco no JSON
- ✅ **Custo-benefício:** Não requer infraestrutura adicional (grafos, vetores)

## 🔗 Referências
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Reflection API Java](https://docs.oracle.com/javase/tutorial/reflect/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/actuator/htmlsingle/)
- [Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
