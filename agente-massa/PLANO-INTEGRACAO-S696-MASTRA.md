# Plano de integração do agente S696 ao Mastra

**Status:** proposta para revisão  
**Escopo:** integrar as capacidades de geração de massa do `agente-s696` à aplicação Mastra em `agente-massa`.

## 1. Objetivo

Permitir que usuários solicitem em linguagem natural a geração de previews e scripts SQL para o S696 por meio do agente Mastra, reutilizando as regras de domínio já implementadas e mantendo a geração separada da execução no banco.

## 2. Contexto atual

- `agente-s696` contém parsing de CSV, inferência e validação de templates, transformação de campos, criação de preview, geração SQL, persistência de templates e interpretação de instruções em português.
- `agente-s696/src/cli/index.ts` coordena esses recursos, mas também depende de terminal interativo e manipulação local de arquivos. A CLI não deve ser chamada como ferramenta do agente.
- `agente-massa` é uma aplicação Mastra com um agente genérico, ferramentas já registradas em `src/mastra/index.ts`, armazenamento LibSQL e memória.
- Os projetos usam Node.js `>=22.13.0`.
- O pacote S696 atualmente declara `src/index.ts` como ponto de entrada, embora seu script de build gere os artefatos TypeScript em `dist`. A forma de integração entre os projetos deve ser validada antes de implementar as ferramentas.

## 3. Decisões propostas para o MVP

1. Mastra será a interface conversacional e a camada de orquestração; S696 continuará sendo a camada de domínio.
2. O fluxo será síncrono: entrada CSV → template → esclarecimentos → preview → SQL para revisão.
3. O MVP **não executará SQL** nas tabelas de negócio.
4. Persistência de templates será uma ação explícita e separada da geração.
5. A entrada de arquivos será limitada a um mecanismo e diretório controlados. Ferramentas não receberão caminhos arbitrários do sistema operacional.
6. A lógica de domínio não será duplicada em instruções do modelo. O agente deve chamar ferramentas e apresentar os resultados delas.

## 4. Arquitetura proposta

```text
Usuário
  ↓ linguagem natural + CSV
Agente Mastra
  ├── ferramenta de preparação do template
  ├── ferramenta de preview
  ├── ferramenta de geração SQL
  └── ferramenta opcional de persistência explícita
       ↓ imports tipados, sem invocar shell ou CLI
Biblioteca de domínio S696
  ├── parser e inferência
  ├── interpretação determinística de instruções
  ├── validação e transformações
  ├── preview e geração SQL
  └── repositório PostgreSQL (somente quando a operação exigir)
```

As ferramentas devem receber e retornar dados estruturados. Erros de validação, ambiguidades, falhas de conexão e falhas de persistência devem ser devolvidos como erros explícitos; não devem ser convertidos em respostas de sucesso.

## 5. Plano de execução

### Etapa 1 — Definir o contrato de integração

- Confirmar o conteúdo e os limites do CSV aceito pela interface Mastra.
- Definir se a primeira versão receberá arquivo por upload do Studio ou por conteúdo enviado em uma área de entrada controlada.
- Definir os formatos de entrada e saída das ferramentas: conteúdo/identificador de arquivo, autor, parâmetros, resumo do template, preview e SQL.
- Decidir se o MVP inclui persistência de templates ou se ela fica para uma entrega posterior.

**Resultado esperado:** contrato das ferramentas aprovado antes de implementar qualquer integração.

### Etapa 2 — Expor casos de uso independentes da CLI

- Identificar funções existentes reutilizáveis no S696, incluindo `parseCsvContent`, `inferTemplate`, `parseInstruction`, `validateTemplate`, `buildPreview` e `buildApplyScript`.
- Extrair operações de aplicação que recebam dados e opções tipadas, sem dependência de `stdin`, `stdout`, `readline`, `process.argv` ou caminhos de saída.
- Manter a CLI atual como adaptador e preservar seus comportamentos.
- Disponibilizar os casos de uso por uma API pública estável do S696.

**Resultado esperado:** regras reutilizáveis pelo Mastra sem chamar a CLI nem manter uma segunda implementação.

### Etapa 3 — Resolver o empacotamento e a dependência local

- Escolher entre workspace npm na raiz do repositório e dependência de pacote local.
- Ajustar metadados e configuração do pacote S696 para que o import resolva os artefatos gerados pelo build, incluindo tipos TypeScript.
- Confirmar que os comandos de desenvolvimento e build do Mastra conseguem localizar e empacotar a biblioteca S696.
- Evitar copiar arquivos de domínio entre os projetos.

**Resultado esperado:** import do S696 funcionando em desenvolvimento e build, sem alterar a execução independente da CLI.

### Etapa 4 — Implementar e registrar ferramentas Mastra

- Criar ferramentas tipadas para:
  1. Preparar um template a partir de CSV e autor.
  2. Gerar preview com parâmetros e transformações validados.
  3. Gerar SQL a partir do template e do preview.
  4. Persistir template ou versão de field map, se aprovado na Etapa 1.
- Validar os argumentos na fronteira da ferramenta e retornar apenas os dados necessários ao próximo passo.
- Registrar as ferramentas e o agente em `agente-massa/src/mastra/index.ts`, seguindo o padrão local.
- Não expor ferramenta de execução SQL no MVP.

**Resultado esperado:** ferramentas acessíveis pelo agente e testáveis independentemente da conversa.

### Etapa 5 — Configurar o comportamento conversacional

- Substituir ou complementar as instruções do agente genérico em `agente-massa/src/mastra/agents/agent.ts`.
- Orientar o agente a solicitar autor, quantidade de ações/etapas e outros dados necessários antes de gerar.
- Reutilizar `parseInstruction` para extrair valores determinísticos de instruções em português.
- Quando houver ambiguidade — como quantidade por unidade versus total — perguntar ao usuário e não presumir uma resposta.
- Apresentar preview e SQL de forma clara, destacando que o script deve ser revisado antes de execução.

**Resultado esperado:** conversa previsível, sem inventar valores ausentes ou declarar ações que as ferramentas não realizaram.

### Etapa 6 — Segurança, persistência e configuração

- Restringir leitura de CSV à estratégia de upload/área de entrada aprovada.
- Manter credenciais fora do código e documentar as variáveis de ambiente necessárias.
- Preservar a validação de TLS existente para PostgreSQL; não permitir desativação de certificado para bancos não descartáveis.
- Exigir solicitação explícita para salvar templates e transformações.
- Garantir que o SQL gerado seja somente retornado ao usuário e não enviado ao banco automaticamente.

**Resultado esperado:** integração sem execução de SQL de negócio e sem ampliação desnecessária de acesso a arquivos ou credenciais.

### Etapa 7 — Testes, documentação e aceite

- Manter e executar os testes existentes do S696.
- Adicionar testes para os casos de uso extraídos e para schemas, sucesso e erros das ferramentas Mastra.
- Testar ambiguidades, conflito entre instrução e parâmetros, entradas inválidas, CSV inválido e indisponibilidade do banco quando houver persistência.
- Validar build dos dois projetos, inicialização do Mastra e fluxo no Mastra Studio.
- Atualizar os READMEs para descrever instalação, variáveis de ambiente, fluxo e limites de segurança.

**Resultado esperado:** build e testes aprovados e instruções de uso atualizadas.

## 6. Principais arquivos previstos

| Área | Arquivos |
|---|---|
| Casos de uso S696 | `agente-s696/src/` e `agente-s696/src/index.ts` |
| CLI preservada | `agente-s696/src/cli/index.ts` |
| Ferramentas Mastra | novos módulos em `agente-massa/src/mastra/tools/` |
| Agente conversacional | `agente-massa/src/mastra/agents/agent.ts` |
| Registro Mastra | `agente-massa/src/mastra/index.ts` |
| Empacotamento | `package.json` e configuração TypeScript/build dos dois projetos; workspace na raiz se essa opção for aprovada |
| Documentação | `agente-s696/README.md` e `agente-massa/README.md` |
| Testes | `agente-s696/tests/` e testes novos no projeto Mastra |

Os caminhos e a quantidade de arquivos podem mudar após a decisão sobre o contrato de entrada e o empacotamento.

## 7. Critérios de aceite

- O agente recebe um CSV pelo mecanismo aprovado e solicita os dados obrigatórios ausentes.
- A inferência, validação, preview e geração SQL são executados pela lógica do S696.
- Instruções ambíguas ou conflitantes são esclarecidas, não presumidas.
- O SQL gerado é apresentado para revisão e não é executado automaticamente.
- Nenhum caminho arbitrário do host é aceito como entrada de arquivo.
- Persistência, se incluída, exige uma ação explícita e utiliza configuração segura.
- A CLI independente continua funcionando.
- Builds e testes relevantes dos dois projetos passam.

## 8. Riscos e dependências

| Risco | Mitigação |
|---|---|
| O ponto de entrada e os artefatos do pacote S696 podem não corresponder ao build atual. | Validar imports após build e ajustar `exports`, `types` e empacotamento antes de ligar as ferramentas. |
| A ferramenta de upload de CSV depende da interface/versão Mastra instalada. | Confirmar a API e o contrato suportados pela versão do projeto antes de implementar a entrada de arquivo. |
| A CLI mistura fluxo de domínio com I/O e interação no terminal. | Extrair casos de uso puros e manter a CLI como adaptador, coberta por testes de regressão. |
| O fluxo conversacional pode parecer uma autorização para executar o SQL. | Não criar ferramenta de execução no MVP e exibir aviso de revisão em toda resposta com script. |
| Persistência depende de banco e credenciais que podem não estar disponíveis em desenvolvimento. | Torná-la opcional no fluxo e cobrir falhas de configuração/conexão com erros explícitos e testes isolados. |

## 9. Fora de escopo no MVP

- Executar automaticamente SQL em bancos de negócio.
- Conectar o agente a bancos de produção.
- Criar ou alterar entidades JPA ou os servidores MCP Java.
- Reescrever a lógica de negócio do S696 em prompts ou em um segundo serviço.
- Permitir que o agente execute comandos shell para operar a CLI.

## 10. Decisões pendentes para revisão

1. A entrada inicial de CSV será upload pelo Mastra Studio ou conteúdo em uma área controlada?
2. O MVP deve incluir persistência de templates no PostgreSQL ou limitar-se a preview e SQL?
3. A dependência será organizada como workspace npm na raiz ou como pacote local independente?
4. A geração deve salvar arquivos SQL no workspace do Mastra ou retornar o conteúdo diretamente na conversa?
