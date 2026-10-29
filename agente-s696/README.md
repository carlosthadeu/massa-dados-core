# Agente S696

Projeto Node.js/TypeScript para aprender templates a partir de CSVs e gerar
scripts SQL destinados ao schema PostgreSQL `adm`.

O agente gera scripts para revisão; não executa scripts nem grava linhas nas
tabelas de destino automaticamente.

## Pré-requisitos

- Node.js 22.13 ou superior
- Acesso PostgreSQL com TLS e o certificado CA fornecido pelo provedor

## Preparar o ambiente

Na pasta `agente-s696`, instale as dependências:

```powershell
npm ci
```

Configure `.env` localmente com a URL PostgreSQL, o schema `adm` e o caminho
do certificado CA. Use uma URL sem o parâmetro `sslmode`: a conexão deve
configurar o certificado CA e validar a cadeia TLS. Nunca desative a validação
do certificado. `.env` é ignorado pelo Git; não compartilhe credenciais.

`DATABASE_SSL_CA` deve apontar para o certificado CA confiável do servidor.
`.env.example` contém apenas valores ilustrativos e não é uma configuração de
conexão pronta para uso.

Antes do primeiro uso, crie as tabelas de catálogo no PostgreSQL executando
[`src/persistence/schema.sql`](./src/persistence/schema.sql) com um usuário que
tenha permissão para criar tabelas no schema `adm`.

## Fase 1: inferir e gerar um template

A CLI recebe um arquivo que segue o template:

````markdown
# [Título do Conjunto de Dados / Cenário]

## [Nome da Entidade 1 / Tabela]

### DDL

```sql
-- DDL de criação e constraints da tabela
CREATE TABLE NOME_TABELA (
    -- Definição de colunas e constraints
);
```

### CSV

```plaintext
"CAMPO_1","CAMPO_2","CAMPO_N"
-- linhas de dados
```

---

## [Nome da Entidade 2 / Tabela]

### DDL

```sql
-- DDL de criação e constraints da tabela
CREATE TABLE NOME_TABELA (
    -- Definição de colunas e constraints
);
```

### CSV

```plaintext
"CAMPO_1","CAMPO_2","CAMPO_N"
-- linhas de dados
```

---

## [Nome da Entidade N / Tabela]

### DDL

```sql
-- DDL de criação e constraints da tabela
CREATE TABLE NOME_TABELA (
    -- Definição de colunas e constraints
);
```

### CSV

```plaintext
"CAMPO_1","CAMPO_2","CAMPO_N"
-- linhas de dados
```
````


```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'primaryColor': '#1e293b', 'primaryTextColor': '#f8fafc', 'primaryBorderColor': '#334155', 'lineColor': '#64748b'}}}%%
graph LR
    DOC["<b>Conjunto de Dados</b><br/><i>(Análise Diretoria)</i>"]

    %% Portfolio
    DOC --> S1["<b>Portfolio</b>"]
    S1 --> S1_D["DDL"] --> S1_D_C["Bloco SQL"]
    S1 --> S1_C["CSV"] --> S1_C_C["Bloco Plaintext"]

    %% Ação Estratégica
    DOC --> S2["<b>Ação Estratégica</b>"]
    S2 --> S2_D["DDL"] --> S2_D_C["Bloco SQL"]
    S2 --> S2_C["CSV"] --> S2_C_C["Bloco Plaintext"]

    %% Etapas
    DOC --> S3["<b>Etapas da Ação Estratégica</b>"]
    S3 --> S3_D["DDL"] --> S3_D_C["Bloco SQL"]
    S3 --> S3_C["CSV"] --> S3_C_C["Bloco Plaintext"]

    %% Vinculação
    DOC --> S4["<b>Vinculação Orientação</b>"]
    S4 --> S4_D["DDL"] --> S4_D_C["Bloco SQL"]
    S4 --> S4_C["CSV"] --> S4_C_C["Bloco Plaintext"]

    %% Domínio
    DOC --> S5["<b>Domínio Ação Estratégica</b>"]
    S5 --> S5_D["DDL"] --> S5_D_C["Bloco SQL"]
    S5 --> S5_C["CSV"] --> S5_C_C["Bloco Plaintext"]

    %% Entrega
    DOC --> S6["<b>Entrega Etapas</b>"]
    S6 --> S6_D["DDL"] --> S6_D_C["Bloco SQL"]
    S6 --> S6_C["CSV"] --> S6_C_C["Bloco Plaintext"]

    %% APID
    DOC --> S7["<b>APID</b>"]
    S7 --> S7_D["DDL"] --> S7_D_C["Bloco SQL"]
    S7 --> S7_C["CSV"] --> S7_C_C["Bloco Plaintext"]
    
```

```powershell
npm run build
npm start -- --csv "project-docs/cvs-exemplo/Portfolio analise diretoria.md" --author "Seu nome" --generate
```

O template e os exemplos são salvos em `adm.model_template` e
`adm.model_example` em uma transação. O SQL revisável é escrito em
`output/apply_template_<id>.sql`; por padrão, a CLI não executa esse script no
banco de negócio. Use `--output <caminho>` para escolher outro destino. A
inferência do exemplo registra 8 ações, 4 em cada unidade, status `10` (Análise
Diretoria) e a distribuição observada de etapas por ação.

## Fase 2: preview, parâmetros e transformações

Aplique novamente [`src/persistence/schema.sql`](./src/persistence/schema.sql)
para criar a tabela de versões do field map. O preview usa IDs simulados,
expande ou reduz ações/etapas e mantém os relacionamentos entre portfólio,
ações, etapas e entregas sem consultar o banco de destino.

```powershell
npm start -- --csv "project-docs/cvs-exemplo/Portfolio analise diretoria.txt" --author "Seu nome" --preview --generate --params ".\params.json"
```

Conteúdo de `params.json`:

```json
{
  "actionsCount": 5,
  "stepsPerAction": 2,
  "ensureDelayedStep": true
}
```

O modo CSV com `--preview` e/ou `--generate` não acessa o banco; use
`--save-template` para também persistir o template. Para recuperar um template
já salvo, use `--template-id 1`. `--params` também aceita o caminho para um
arquivo JSON. Para substituir uma transformação por coluna, informe um JSON
(ou arquivo JSON) para `--transforms`, usando chaves `TABELA.COLUNA`.
Transformações disponíveis:

```json
{
  "T696ETAC.AA_REA": { "type": "integer" },
  "T696POAC.NM_PTF": { "type": "default", "value": "Portfolio de teste" },
  "T696ACES.ST_ACO_ETT": {
    "type": "valueMap",
    "values": { "10": "10", "20": "20" },
    "fallback": "10"
  }
}
```

Tipos disponíveis: `integer`, `number`, `timestamp`, `date` (`YYYY-MM-DD`),
`string`, `default` e `valueMap`. Cada substituição válida cria uma nova versão
em `adm.model_field_map_version`.

Por segurança, `--apply` só executa o SQL gerado depois de confirmação em um
terminal interativo, digitando exatamente `APLICAR <nome do portfólio>`.
Scripts podem continuar sendo gerados para revisão sem execução.

## Fase 3: instruções em linguagem natural

`--instr` aceita instruções em português, extrai quantidade de ações/etapas,
nome do portfólio e as flags “próximo ano” e “atrasada”, e gera o SQL sem
salvar o template ou conectar ao PostgreSQL. Combine com um CSV e autor ou com
um template já salvo:

```powershell
npm start -- --csv "project-docs/cvs-exemplo/Portfolio analise diretoria.txt" --author "Seu nome" --instr "Crie um portfólio chamado `"Diretoria 2026`" com 5 ações, 2 etapas por ação para o próximo ano e uma etapa atrasada."
npm start -- --template-id 1 --instr "Gere 6 ações com 3 etapas por ação" --preview ".\output\preview.json"
```

Uma instrução vaga como “gere algumas ações” pede a quantidade no terminal
interativo; em execução não interativa, a CLI retorna uma mensagem informando
qual detalhe deve ser especificado. Menções como “4 ações por unidade” também
pedem esclarecimento entre total do portfólio e quantidade por unidade. A
etapa “próximo ano” usa ano de realização +1; a etapa “atrasada” usa ano -1.
As flags são independentes e podem ser combinadas.

O exemplo [`exemplo-instrucao.txt`](./project-docs/exemplo-instrucao.txt) usa o
CSV descrito em [`analise-diretoria-csv.md`](./project-docs/cvs-exemplo/analise-diretoria-csv.md).
O SQL resultante é salvo em `output/` por padrão e deve ser revisado antes de
aplicação.

## Fase 4: testes PostgreSQL e execução no DBeaver

Os testes unitários cobrem parser CSV, inferência, SQL e instruções. Para rodar
o teste de integração, inicie o PostgreSQL descartável local com Docker Compose:

```powershell
docker compose -f docker-compose.yml up -d --wait
$env:TEST_DATABASE_URL = "postgres://postgres:local-test-only@127.0.0.1:55432/agente_s696_test"
npm run test:integration
docker compose -f docker-compose.yml down
Remove-Item Env:TEST_DATABASE_URL
```

O teste exige uma URL local cujo nome contenha `test`, usa um schema temporário
para as tabelas de destino e remove o template de teste ao terminar. O serviço
Compose vincula a porta somente a `127.0.0.1` e mantém os dados em `tmpfs`; as
credenciais são exclusivas para esse banco descartável. Sem `TEST_DATABASE_URL`,
o teste de integração é ignorado, mas `npm test` continua executando os testes
unitários. O comando `npm run test:integration` falha explicitamente se
`TEST_DATABASE_URL` não estiver configurada.

Para executar o SQL manualmente no DBeaver:

1. Gere o arquivo com `--generate` ou `--instr` e abra-o no editor SQL.
2. Selecione a conexão e confirme que o `search_path` aponta para os schemas
   corretos das tabelas `T696POAC`, `T696ACES`, `T696ETAC`, `T696VIAC`,
   `T696DOAC` e `T696ENAC`.
3. Revise o bloco `DO`, os dados e as relações entre IDs antes de executar o
   script completo. Ele usa uma transação e contém `COMMIT`.
4. Confira as linhas inseridas nas tabelas de destino antes de usar os dados.

Não execute o script em produção sem revisão e autorização do responsável pelo
banco. A opção `--apply` também requer a confirmação interativa exata
`APLICAR <nome do portfólio>`; o SQL continua disponível para inspeção manual.

## Conhecimento de domínio (carga offline)

Ferramenta separada do chat, em `src/knowledge`, que carrega documentos
(`.md`, `.txt`, `.sql`) em tabelas `conhecimento_*` do schema configurado
(`DATABASE_SCHEMA`). Veja o plano em `agente-massa/PLANO-PERSISTENCIA-CONHECIMENTO.md`.

```powershell
npm run build
npm run conhecimento -- migrar
npm run conhecimento -- carregar project-docs --dry-run
npm run conhecimento -- carregar project-docs --executor "seu-nome"
npm run conhecimento -- pendencias
npm run conhecimento -- listar-cargas
npm run conhecimento -- verificar
```

- A carga é incremental: arquivo com o mesmo hash é pulado (use `--forcar`),
  conteúdo alterado gera nova versão do item e itens ausentes do documento são
  apenas reportados.
- Lacunas (por exemplo, estados sem código) ficam como `pendente` e aparecem
  em `pendencias`.
- Mudanças no DDL de uma tabela aparecem no relatório da carga.
- Blocos CSV ainda são ignorados com aviso (Fase 3); embeddings virão na Fase 4.
- `--dry-run` compara com o banco sem gravar; sem conexão, só analisa os arquivos.

## Verificações

```powershell
npm run build
npm run lint
npm run format:check
npm test
npm run test:integration
```
