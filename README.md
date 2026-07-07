# Massa Dados Core

> Servidor MCP para geração de massa de dados a partir de scripts DDL e metadados de entidades JPA.

[![Versão](https://img.shields.io/badge/version-1.0.0-blue)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

---

## 📋 Sobre

Este projeto implementa dois servidores MCP (Model Context Protocol) usando Spring Boot:

- **Servidor 1 — Configuração e DDL**: Recebe scripts DDL, gera classes Entity JPA correspondentes e recompila o segundo servidor.
- **Servidor 2 — Metadados e Massa de Dados**: Analisa a estrutura das entidades via reflection e retorna um dicionário de metadados para que o cliente MCP (Continue) possa criar massa de dados usando linguagem natural.

### Problema

Criar massa de dados para homologação manualmente é demorado e propenso a erros. O time de negócio precisa de uma forma simples de gerar dados de teste sem conhecer a estrutura interna do banco.

### Solução

Um sistema que:
1. Recebe scripts DDL e gera automaticamente as classes Entity JPA
2. Expõe metadados das entidades via MCP
3. Permite que o usuário (via chat MCP) peça criação de massa de dados em linguagem natural

---

## 🚀 Começando

### Pré-requisitos

- Java 21
- Maven 3.8+
- Git

### Instalação

```bash
# Clone o repositório
git clone https://github.com/thadeugarrido/massa-dados-core.git
cd massa-dados-core

# Compile o projeto
mvn clean compile

# Execute o servidor
mvn spring-boot:run
```

---

## 🏗️ Arquitetura

```
src/
├── main/java/com/thadeu/massa_dados_core/
│   ├── App.java
│   ├── config/
│   ├── domain/
│   ├── mcp/
│   │   ├── dto/
│   │   ├── McpServerConfig.java
│   │   ├── McpToolHandler.java
│   │   ├── McpDdlToEntityService.java
│   │   ├── McpUnknownEntityService.java
│   │   └── McpCompileService.java
│   └── resources/
└── test/
```

---

## 🔧 Funcionalidades

- [x] Conversão de DDL para classes Entity JPA
- [x] Identificação de tabelas/colunas sem Entity correspondente
- [x] Recompilação automática do servidor após geração
- [ ] Obtenção de metadados de entidades via reflection
- [ ] Geração de massa de dados via linguagem natural

---

## 🧪 Testes

```bash
mvn test       # Unitários
mvn verify     # Integração
```

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).

---

## ✉️ Contato

- **Autor:** Thadeu Garrido
- **Email:** [thadeu@example.com]
