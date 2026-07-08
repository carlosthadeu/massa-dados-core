# Massa Dados Core - Servidores MCP

> Sistema de dois servidores MCP (Model Context Protocol) para geração de massa de dados a partir de scripts DDL e consultas em linguagem natural.

[![Versão](https://img.shields.io/badge/version-1.0.0-blue)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

---

## 📋 Sobre

### Problema

Equipes de homologação precisam criar massa de dados de teste para validar funcionalidades, mas:
- O banco de produção tem dezenas de tabelas com relacionamentos complexos
- As entidades JPA estão desatualizadas ou incompletas
- Não há uma forma simples de consultar dados existentes usando linguagem natural

### Solução

Dois servidores MCP que trabalham em conjunto:

1. **configuracao-ddl-mcp** (porta 8081): Converte scripts DDL em classes Entity JPA automaticamente, identifica tabelas/colunas não reconhecidas e recompila o projeto.

2. **metadados-massa-mcp** (porta 8082): Expõe metadados das entidades, permite consultas em linguagem natural via mapeamento semântico, cria massa de dados estruturada e gera demandas automaticamente quando não consegue responder.

---

## 🚀 Começando

### Pré-requisitos

- Java 21 (JDK)
- Apache Maven 3.9+
- VS Code com extensões Java e Spring Boot

### Instalação

```bash
# Clone o repositório
git clone <url-do-repositorio>
cd massa-dados-core

# Compilar o módulo de entidades compartilhadas
cd entities-core
mvn clean install -DskipTests
cd ..

# Compilar cada servidor
cd configuracao-ddl-mcp
mvn clean install -DskipTests
cd ..

cd metadados-massa-mcp
mvn clean install -DskipTests
cd ..
```

### Execução

```bash
# Terminal 1 - Servidor de Configuração DDL
cd configuracao-ddl-mcp
mvn spring-boot:run

# Terminal 2 - Servidor de Metadados
cd metadados-massa-mcp
mvn spring-boot:run
```

---

## 🏗️ Arquitetura

```
Cliente MCP (Continue)
    ↓ (JSON-RPC via HTTP)
configuracao-ddl-mcp (porta 8081)
    ├── McpServerConfig
    ├── McpToolHandler
    ├── McpDdlToEntityService
    ├── McpUnknownEntityService
    ├── McpCompileService
    └── dto/
metadados-massa-mcp (porta 8082)
    ├── McpServerConfig
    ├── McpToolHandler
    ├── McpEntityMetadataService
    ├── McpMapeamentoSemanticoService
    ├── McpConsultaService
    ├── McpDemandaService
    └── dto/
entities-core (compartilhado)
    └── br/gov/bnb/domain/entity/
```

---

## 🔧 Funcionalidades

- [x] Conversão de DDL para Entity JPA
- [x] Identificação de tabelas/colunas não reconhecidas
- [x] Recompilação automática do projeto
- [x] Metadados de entidades via reflection
- [x] Mapeamento semântico (JSON + sinônimos)
- [x] Consulta em linguagem natural
- [x] Criação de massa de dados estruturada
- [x] Geração automática de demandas
- [x] Listagem e resolução de demandas

---

## 🧪 Testes

```bash
cd configuracao-ddl-mcp
mvn test

cd metadados-massa-mcp
mvn test
```

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).

---

## ✉️ Contato

- **Autor:** Thadeu Garrido
- **Email:** [thadeu.garrido@exemplo.com](mailto:thadeu.garrido@exemplo.com)
