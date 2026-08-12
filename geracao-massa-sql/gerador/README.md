# Gerador de Massa de Dados SQL

Gerador em TypeScript de scripts SQL (T-SQL) para criação de massa de dados de **ações estratégicas**, **etapas**, **entregas**, **vínculos** e **domínios** no banco de dados SQL Server.

O script gerado cria automaticamente N ações estratégicas para um portfólio de destino informado, sem depender de um portfólio de origem, com nomes padronizados e valores respeitando as regras de negócio definidas no plano.

## Funcionalidades

- Geração de script SQL completo e transacional (`BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK`)
- Criação de ações estratégicas com nomes padronizados:
  `Portfolio [destino] - Ação Estratégica [NN]`
- Criação de **2 a 5 etapas** por ação (configurável)
- Geração de **2 a 6 entregas** por etapa, com `MM_ENT` respeitando a janela máxima de 6 meses
- Percentuais de planejamento (`PC_PLJ_ENT`) distribuídos para que a soma por etapa seja exatamente **100**
- Criação de vínculos com orientações estratégicas (`SQ_ORT_ETT` = `58` e `60`)
- Associação aleatória de domínio (`SQ_DMN` ∈ {143, 144, 145, 146})
- Data de limite (`DH_LIM`) definida para 31/12 do ano subsequente
- Campo `DE_HST` gerado dinamicamente com histórico de aprovações com horários retroativos à execução
- Texto `lorem ipsum` para descrições (`DE_ACO_ETT`, `DE_ETP_ACO`, `DE_ENT_ETP_ACO`)
- Tabela temporária `#TempIDs` para controle de identidades entre inserts (dispensa `SCOPE_IDENTITY()` explícito)
- Uso de `TRY/CATCH` com rollback automático em caso de erro
- Saída em arquivo `.sql` no diretório `output/`

## Tecnologias

| Tecnologia | Versão |
|------------|--------|
| Node.js | ≥ 18 |
| TypeScript | ^7.0.2 |
| Vitest | ^4.1.10 |

## Estrutura do Projeto

```
geracao-massa-sql/gerador/
├── src/
│   ├── index.ts                      (ponto de entrada e orquestração)
│   ├── types.ts                      (interfaces e tipos)
│   ├── templates/
│   │   ├── acao.ts                   (INSERT em T696ACES)
│   │   ├── etapa.ts                  (INSERT em T696ETAC)
│   │   ├── entrega.ts                (INSERT em T696ENAC)
│   │   ├── vinculoAcao.ts            (INSERT em T696VIAC)
│   │   ├── dominioAcao.ts            (INSERT em T696DOAC)
│   │   ├── historico.ts              (geração do texto de DE_HST)
│   │   └── vinculoDominio.ts         (utilitários para vínculo/domínio)
│   └── utils/
│       ├── sql.ts                    (funções auxiliares: lorem ipsum, meses, percentuais, cabeçalho)
│       └── validators.ts             (validação de parâmetros)
├── tests/
│   └── index.test.ts                 (testes unitários - Vitest)
├── scripts/
│   └── teste-manual.ts               (script de validação manual)
├── output/                           (arquivos .sql gerados)
├── package.json
├── tsconfig.json
├── vitest.config.ts
└── .gitignore
```

## Tabelas Alvo

| Tabela | Descrição | Função Geradora |
|--------|-----------|-----------------|
| `T696ACES` | Ações Estratégicas | `gerarInsertAcao()` |
| `T696VIAC` | Vínculo Ação / Orientação Estratégica | `gerarInsertVinculoAcao()` |
| `T696DOAC` | Domínio da Ação | `gerarInsertDominioAcao()` |
| `T696ETAC` | Etapas da Ação | `gerarInsertEtapa()` |
| `T696ENAC` | Entregas da Etapa | `gerarInsertEntrega()` |

## Instalação

```powershell
Set-Location "geracao-massa-sql/gerador"
npm install
```

## Compilação

```powershell
npx tsc
```

## Uso Resumido

```powershell
node dist/index.js <destino> [quantidadeAcoes]
```

### Exemplos

```powershell
# Cria 5 ações para o portfólio 78 (padrão)
node dist/index.js 78

# Cria 10 ações para o portfólio 99
node dist/index.js 99 10
```

O arquivo gerado será salvo em:

```
output/massa_dados_ptf_<destino>.sql
```

Para instruções detalhadas, consulte o arquivo **[USAGE.md](USAGE.md)**.

## Scripts Disponíveis

| Comando | Descrição |
|---------|-----------|
| `npm run build` | Compila o TypeScript para `dist/` |
| `npm run start` | Executa o script compilado |
| `npm run dev` | Compila em modo *watch* |
| `npm test` | Executa os testes com Vitest |

## Estrutura do Script SQL Gerado

O script gerado segue o seguinte fluxo:

1. **Cabeçalho** com parâmetros de geração (portfólio, data)
2. **`BEGIN TRANSACTION`** — toda a carga é atômica
3. **Criação da tabela temporária `#TempIDs`** — armazena os IDs recém-inseridos (`ACO` e `ETP`)
4. Para cada ação:
   - `INSERT` em `T696ACES` (ação)
   - Captura do identificador via `#TempIDs`
   - `INSERT` em `T696VIAC` (2 vínculos: `SQ_ORT_ETT` 58 e 60)
   - `INSERT` em `T696DOAC` (domínio sorteado)
   - Para cada etapa:
     - `INSERT` em `T696ETAC` (etapa)
     - Captura do identificador via `#TempIDs`
     - 2 a 6 `INSERT` em `T696ENAC` (entregas)
5. **`COMMIT TRANSACTION`** + mensagem de sucesso
6. **`BEGIN CATCH`** — em erro, faz `ROLLBACK` e relança a exceção

## Regras de Negócio Implementadas

### Primeira Ação (Ação 01)

- ⚠️ Deve ter **pelo menos 2 etapas** (garantido)
- Etapa 1: `AA_REA` = ano corrente (ex.: 2026)
- Etapa 2: `AA_REA` = ano subsequente (ex.: 2027)
- Etapas adicionais (3..5): ano sorteado entre corrente e subsequente

### Demais Ações (02..N)

- Quantidade de etapas: aleatória entre `minEtapas` e `maxEtapas` (padrão 2..5)
- `AA_REA` sorteado entre ano corrente e subsequente

### Entregas

- Quantidade: 2 a 6 entregas por etapa
- `MM_ENT`: meses dentro de uma janela máxima de **6 meses** entre a primeira e a última entrega
- `PC_PLJ_ENT`: valores positivos cuja soma por etapa é **exatamente 100**
- `ST_ENT_ACO`: `1` (ativo)

### Usuários / Responsáveis

| Código | Perfil | Aplicação |
|--------|--------|-----------|
| `F856806` | Equipe do Ambiente de Planejamento | `MT_USU_ALT` / `NM_USU_ALT` da ação |
| `F856803` | Facilitador 1681 | `MT_USU_ALT` / `NM_USU_ALT` de etapas e entregas |
| `F054135` | Responsável | `MT_USU_RSP` da ação |

### CD_UND (Código da Unidade)

As ações são divididas em duas unidades:

- **Metade 1:** `CD_UND = 1681`
- **Metade 2:** `CD_UND = 955`

O que define o quantitativo exato para cada unidade quando o total de ações é ímpar.

### CD_UND_APR (Unidade Aprovação)

Valor fixo: `694`.

## Testes

> ⚠️ **Atenção:** os testes em `tests/index.test.ts` estão **desatualizados** — fazem referência a uma classe `CodeGenerator` que não existe mais no `src/index.ts`. A suíte atualmente falha com `TypeError: CodeGenerator is not a constructor`. Os testes precisam ser reescritos para cobrir as funções atuais: `gerarScript()`, `gerarInsertAcao()`, `gerarInsertEtapa()`, `gerarInsertEntrega()`, `gerarMeses()`, `gerarPercentuais()`, etc.

## Limitações Conhecidas

- O `DE_HST` gerado por `gerarTextoHistorico()` é o **mesmo** para todas as ações dentro de uma mesma execução, pois `gerarInsertAcao()` calcula um novo histórico a cada chamada — porém, os horários são calculados a partir do mesmo `new Date()`, resultando em timestamps idênticos em ações diferentes.
- Validação de parâmetros é mínima: apenas verifica se `<destino>` é numérico. Não valida faixas aceitáveis (ex.: `minEtapas <= maxEtapas`, quantidades positivas, etc.).
- Não existe suporte de entrada via arquivo de configuração — apenas linha de comando.
- O script gerado deve ser executado em um banco SQL Server que já contenha as tabelas-alvo e registros de orientações estratégicas (`SQ_ORT_ETT` 58/60) e domínios (`SQ_DMN` 143–146).
- Os testes atuais estão quebrados (ver seção [Testes](#testes)).

## Contribuição

1. Faça o *fork* do projeto
2. Crie uma *branch* descritiva: `git checkout -b feature/nome-da-feature`
3. Faça as alterações seguindo as regras do projeto
4. Rode os testes: `npm test`
5. Envie um *pull request*

## Licença

Projeto interno — uso exclusivo da equipe de desenvolvimento.