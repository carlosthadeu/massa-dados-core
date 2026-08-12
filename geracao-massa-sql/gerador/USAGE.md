# USAGE — Gerador de Massa de Dados SQL

Guia de utilização completo do gerador de massa de dados SQL.

## Requisitos

- **Node.js** versão 18 ou superior
- **npm** (gerenciador de pacotes)
- Acesso a um banco **SQL Server** com as tabelas-alvo existentes:
  - `T696ACES` (Ações Estratégicas)
  - `T696VIAC` (Vínculo Ação × Orientação Estratégica)
  - `T696DOAC` (Domínio da Ação)
  - `T696ETAC` (Etapas)
  - `T696ENAC` (Entregas)

## Instalação

### 1. Instalar dependências

```powershell
Set-Location "geracao-massa-sql/gerador"
npm install
```

### 2. Compilar o TypeScript

```powershell
npx tsc
```

O código compilado será gerado no diretório `dist/`.

---

## Interface de Linha de Comando (CLI)

### Sintaxe

```
node dist/index.js <destino> [quantidadeAcoes]
```

### Parâmetros

| Parâmetro | Obrigatório | Tipo | Padrão | Descrição |
|-----------|-------------|------|--------|-----------|
| `<destino>` | ✅ Sim | `number` | — | ID do portfólio de destino (código do PTF) |
| `[quantidadeAcoes]` | ❌ Não | `number` | `5` | Quantidade de ações estratégicas a criar |

> ⚠️ Parâmetros adicionais (`minEtapas`, `maxEtapas`, `valoresFixos`) só podem ser configurados via código (veja a seção [Uso via API](#uso-via-api-typescript)).

### Exemplos

#### Exemplo 1: Gerar script para o portfólio 78 com 5 ações (padrão)

```powershell
node dist/index.js 78
```

Saída:

```
Script SQL gerado com sucesso em: <caminho>/output/massa_dados_ptf_78.sql
```

#### Exemplo 2: Gerar script para o portfólio 99 com 10 ações

```powershell
node dist/index.js 99 10
```

Saída:

```
Script SQL gerado com sucesso em: <caminho>/output/massa_dados_ptf_99.sql
```

#### Exemplo 3: Execução sem argumentos (mostra a ajuda)

```powershell
node dist/index.js
```

Saída:

```
Uso: node dist/index.js <destino> [quantidadeAcoes]
Exemplo: node dist/index.js 78 10
```

#### Exemplo 4: Argumento inválido

```powershell
node dist/index.js abc
```

Saída:

```
Erro: O parâmetro <destino> deve ser um número.
Uso: node dist/index.js <destino> [quantidadeAcoes]
```

O processo encerra com código de saída `1`.

---

## Arquivo de Saída

O script SQL gerado é salvo como:

```
output/massa_dados_ptf_<destino>.sql
```

O diretório `output/` é criado automaticamente caso não exista.

### Exemplos de arquivos gerados

| Comando | Arquivo gerado |
|---------|----------------|
| `node dist/index.js 77` | `output/massa_dados_ptf_77.sql` |
| `node dist/index.js 78 10` | `output/massa_dados_ptf_78.sql` |

---

## Uso via API (TypeScript)

Além da CLI, é possível usar as funções do gerador diretamente em código TypeScript/JavaScript.

### Função `gerarScript()`

```typescript
import { gerarScript } from './src/index.js';

const script = gerarScript({ destino: 78, quantidadeAcoes: 10 });
console.log(script);
```

### Interface `GeradorParams`

```typescript
interface GeradorParams {
  /** ID do portfólio de destino (obrigatório) */
  destino: number;
  /** Quantidade de ações (padrão: 5) */
  quantidadeAcoes?: number;
  /** Quantidade mínima de etapas por ação (padrão: 2) */
  minEtapas?: number;
  /** Quantidade máxima de etapas por ação (padrão: 5) */
  maxEtapas?: number;
  /** Valores fixos que sobrescrevem os padrões */
  valoresFixos?: ValoresFixos;
}
```

### Exemplo com valores fixos personalizados

```typescript
import { gerarScript } from './src/index.js';

const script = gerarScript({
  destino: 78,
  quantidadeAcoes: 3,
  minEtapas: 3,
  maxEtapas: 4,
  valoresFixos: {
    stAcoEtt: 10,      // Status da ação
    stEtpAco: 1,       // Status da etapa
    stEntAco: 1,       // Status da entrega
    pcReaTot: 0,       // Percentual de realização total
    stRep: 0,          // Status de reprovação
  },
});
```

### Uso de funções isoladas

```typescript
import { gerarMeses, gerarPercentuais, gerarLoremIpsum } from './src/utils/sql.js';
import { gerarInsertAcao } from './src/templates/acao.js';
import { gerarInsertEtapa } from './src/templates/etapa.js';
import { gerarInsertEntrega } from './src/templates/entrega.js';
import { gerarInsertVinculoAcao } from './src/templates/vinculoAcao.js';
import { gerarInsertDominioAcao } from './src/templates/dominioAcao.js';

// Exemplo: gerar apenas uma ação
const acaoSql = gerarInsertAcao(78, 1, 1681);

// Exemplo: gerar meses para 4 entregas
const meses = gerarMeses(4);  // ex.: [3, 5, 7, 9]

// Exemplo: gerar percentuais que somam 100
const percentuais = gerarPercentuais(4);  // ex.: [24, 36, 15, 25]
```

---

## Como Executar o Script SQL Gerado

1. Abra o **SQL Server Management Studio (SSMS)** ou outra ferramenta de consulta
2. Selecione o banco de dados de destino
3. Abra o arquivo gerado:
   ```
   output/massa_dados_ptf_<destino>.sql
   ```
4. Execute (`F5` ou *Execute*)

O script é **transacional**:

- Se tudo ocorrer bem:
  ```
  Carga de massa de dados realizada com sucesso.
  ```
- Se ocorrer erro:
  ```
  Erro ao executar a carga de dados. Operação cancelada.
  ```
  E a transação é revertida (`ROLLBACK`), não deixando dados parciais.

### Pré-requisitos no banco de destino

Antes de executar, certifique-se de que:

- As tabelas `T696ACES`, `T696VIAC`, `T696DOAC`, `T696ETAC` e `T696ENAC` existem
- Existem registros nas orientações estratégicas (tabela de `SQ_ORT_ETT`) com IDs `58` e `60`
- Existem domínios com IDs `143`, `144`, `145` e `146` na tabela de domínios (`SQ_DMN`)
- A unidade `694` existe como unidade de aprovação (`CD_UND_APR`)
- As unidades `1681` e `955` existem como unidades executoras (`CD_UND`)
- O usuário de banco tem permissão de `INSERT` nas tabelas-alvo

---

## Regras Aplicadas na Geração

### Ações (T696ACES)

| Campo | Valor | Notas |
|-------|-------|-------|
| `SQ_PTF_ACO` | `<destino>` | Portfólio de destino |
| `SQ_ORI_ACO` | `1` | Origem fixa |
| `CD_UND` | `1681` (1ª metade) ou `955` (2ª metade) | Baseado na ordem da ação |
| `CD_UND_APR` | `694` | Unidade de aprovação fixa |
| `NM_ACO_ETT` | `Portfolio <destino> - Ação Estratégica <NN>` | Nome padronizado |
| `DE_ACO_ETT` | Lorem ipsum | Descrição |
| `ST_ACO_ETT` | `10` | Status ativo |
| `TP_ACO_ETT` | `3` | Tipo fixo |
| `MT_USU_ALT` | `F856806` | Usuário de alteração |
| `NM_USU_ALT` | `Equipe do Ambiente de Planejamento` | Nome do usuário |
| `MT_USU_RSP` | `F054135` | Responsável |
| `DH_LIM` | `31/12/<anoAtual+1> 23:59:59` | Data limite dinâmica |
| `DE_HST` | Histórico com 6 passos | Gerado com horários retroativos |
| `DH_ALT` | `GETDATE()` | Data de alteração |

### Etapas (T696ETAC)

| Campo | Valor | Notas |
|-------|-------|-------|
| `SQ_ACO_ETT` | `(SELECT ID FROM #TempIDs WHERE Tipo = 'ACO')` | ID da ação via tabela temporária |
| `CD_UND` | Herdado da ação | Unidade da ação |
| `DE_ETP_ACO` | `Portfolio <destino> - Ação Estratégica <NN> - Etapa <NN>` | Nome padronizado |
| `DE_ENT_ETP_ACO` | Lorem ipsum | Descrição da entrega da etapa |
| `ST_ETP_ACO` | `1` | Status ativo |
| `AA_REA` | Ano corrente ou subsequente | Ver regras abaixo |
| `PC_REA_TOT` | `0` | Percentual de realização |
| `ST_REP` | `0` | Status reprovação |
| `MT_USU_ALT` | `F856803` | Facilitador 1681 |
| `NM_USU_ALT` | `Facilitador 1681` | Nome do usuário |

### Entregas (T696ENAC)

| Campo | Valor | Notas |
|-------|-------|-------|
| `SQ_ETP_ACO` | `(SELECT ID FROM #TempIDs WHERE Tipo = 'ETP')` | ID da etapa via tabela temporária |
| `MM_ENT` | 1–12 | Dentro de janela de 6 meses |
| `PC_PLJ_ENT` | 1–100 | Soma por etapa = 100 |
| `ST_ENT_ACO` | `1` | Status ativo |
| `MT_USU_ALT` | `F856803` | Facilitador 1681 |
| `NM_USU_ALT` | `Facilitador 1681` | Nome do usuário |

### Regra da Primeira Ação (Ação 01)

| Etapa | Ano (`AA_REA`) |
|-------|---------------|
| Etapa 1 | Ano corrente (ex.: 2026) |
| Etapa 2 | Ano subsequente (ex.: 2027) |
| Etapas 3–5 | Sorteado entre corrente e subsequente |

### Regra das Demais Ações

- Quantidade de etapas: aleatória entre `2` e `5`
- Ano (`AA_REA`): sorteado entre corrente e subsequente

---

## Testes

### Estado atual

> 🚨 **Os testes estão quebrados.**

O arquivo `tests/index.test.ts` importa `CodeGenerator` do `src/index.ts`, mas essa classe **não existe** no código atual. Executar `npm test` resulta em:

```
TypeError: CodeGenerator is not a constructor
```

### Como executar os testes

```powershell
npm test
```

### Como corrigir

Os testes precisam ser reescritos para testar as funções exportadas atuais:

- `gerarScript()` — geração completa
- `gerarInsertAcao()` — INSERT de ação
- `gerarInsertEtapa()` — INSERT de etapa
- `gerarInsertEntrega()` — INSERT de entrega
- `gerarMeses()` — geração de meses
- `gerarPercentuais()` — geração de percentuais
- `gerarLoremIpsum()` — geração de lorem ipsum
- `gerarCabecalho()` — cabeçalho do script

---

## Solução de Problemas

### Erro: `O parâmetro <destino> deve ser um número.`

**Causa:** o primeiro argumento não é numérico.

**Solução:** informe um número inteiro para o destino:

```powershell
node dist/index.js 78
```

### Erro: `Cannot find module './src/index.js'` (em imports)

**Causa:** o código foi executado sem compilar ou sem a extensão `.js` nos imports (ESM).

**Solução:** compile antes de executar:

```powershell
npx tsc
```

### Erro: `File output/massa_dados_ptf_<destino>.sql already exists`

**Causa:** não deve ocorrer pois o arquivo é sobrescrito com `fs.writeFileSync` (comportamento padrão).

### Erro SQL: `Invalid object name 'dbo.T696ACES'`

**Causa:** as tabelas-alvo não existem no banco de dados selecionado.

**Solução:** selecione o banco correto no SSMS ou crie as tabelas antes.

### Erro SQL: `Violation of FOREIGN KEY constraint`

**Causa:** os valores de referência (orientações 58/60, domínios 143–146, unidades 694/1681/955) não existem.

**Solução:** cadastre os registros de referência antes de executar o script.

---

## Referência Rápida de Comandos

```powershell
# Instalar dependências
npm install

# Compilar TypeScript
npx tsc

# Compilar em modo watch
npm run dev

# Executar com 5 ações para o portfólio 78
node dist/index.js 78

# Executar com 10 ações para o portfólio 99
node dist/index.js 99 10

# Executar testes
npm test

# Teste manual (quando os testes forem corrigidos)
node --loader ts-node/esm scripts/teste-manual.ts
```