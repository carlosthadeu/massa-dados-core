# Plano para Criação de Script TypeScript Gerador de Scripts SQL

## Objetivo

Criar um script TypeScript que, a partir de parâmetros de entrada (portfólio de destino, quantidade de ações, etc.), gere automaticamente um script SQL que **cria** 5 ações estratégicas, cada uma com 2 a 5 etapas, com nomes padronizados, sem depender de um portfólio de origem.

## Pré-requisitos

- Node.js instalado (versão 18 ou superior)
- TypeScript instalado globalmente ou como dependência do projeto
- Conhecimento básico de SQL e do esquema das tabelas envolvidas

## Estrutura de Arquivos Sugerida

```
geracao-massa-sql/
├── clonar portfolio S696.sql          (script SQL existente - referência)
├── analise-sql.md                     (análise existente)
├── plano-gerador-script.md            (este arquivo)
├── gerador/
│   ├── package.json
│   ├── tsconfig.json
│   ├── src/
│   │   ├── index.ts                   (ponto de entrada)
│   │   ├── templates/
│   │   │   ├── acao.ts                (template para inserção de ações)
│   │   │   ├── etapa.ts               (template para inserção de etapas)
│   │   │   └── entrega.ts             (template para inserção de entregas - opcional)
│   │   ├── utils/
│   │   │   ├── sql.ts                 (funções auxiliares para geração de SQL)
│   │   │   └── validators.ts          (validação de parâmetros)
│   │   └── types.ts                   (definição de tipos)
│   └── output/                        (diretório onde os scripts gerados serão salvos)
```

## Regras de Desenvolvimento (obrigatórias)

### Formatação (007-formatacao-node.md)

- Indentação: 2 espaços (sem tabs)
- Limite de linha: 100 caracteres
- Ponto e vírgula obrigatório
- Aspas simples (exceto JSX/TSX)
- Linha vazia no final do arquivo
- Final de linha: LF (Unix)

### Nomenclatura (003-nomenclatura-node.md)

- Funções: camelCase (verbo)
- Variáveis: camelCase
- Constantes: UPPER_SNAKE_CASE
- Interfaces: PascalCase (sem prefixo `I`)
- Arquivos: kebab-case
- Componentes React: PascalCase (não aplicável aqui)

### JSDoc/TSDoc (002-jsdoc-node.md)

- Documentar funções exportadas, classes públicas e interfaces
- Usar `@param`, `@returns`, `@throws`
- Primeira linha: descrição curta e objetiva
- Exemplo: `@example` para funções complexas

### Testes (001-testes-node.md)

- Usar Vitest
- Nomear testes como `deve[Acao][Condicao]` (pt-br)
- Estrutura de pastas espelhando `src/`
- Mockar dependências externas com `vi.fn()` ou `vi.mock()`

### SOLID (008-solid-node.md)

- SRP: cada arquivo com responsabilidade única
- OCP: evitar switch/if por tipo; usar configuração externa ou registry pattern
- DIP: injetar dependências via construtor, sem `new` concreto dentro de classes
- ISP: interfaces pequenas e focadas

### Camadas (009-camadas-projeto-node.md)

- Separar em camadas: controller (index.ts), service (gerador), repository (templates)
- Não misturar lógica de negócio com acesso a dados

## Passo a Passo

### 1. Configuração do Projeto TypeScript

- Inicialize um novo projeto Node.js na pasta `gerador`:
  ```bash
  cd geracao-massa-sql/gerador
  npm init -y
  ```
- Instale as dependências necessárias:
  ```bash
  npm install typescript @types/node --save-dev
  npm install vitest --save-dev
  ```
- Crie o arquivo `tsconfig.json` com as seguintes configurações:
  ```json
  {
    "compilerOptions": {
      "target": "ES2020",
      "module": "commonjs",
      "outDir": "./dist",
      "rootDir": "./src",
      "strict": true,
      "esModuleInterop": true,
      "skipLibCheck": true
    },
    "include": ["src/**/*"],
    "exclude": ["node_modules"]
  }
  ```
- Crie o arquivo `vitest.config.ts`:
  ```typescript
  import { defineConfig } from 'vitest/config';

  export default defineConfig({
    test: {
      globals: true,
      environment: 'node',
      include: ['tests/**/*.test.ts'],
    },
  });
  ```

### 2. Definição de Tipos (types.ts)

Crie o arquivo `src/types.ts` com as interfaces que representam os parâmetros de entrada e as estruturas de dados:

```typescript
/**
 * Parâmetros de entrada para o gerador de scripts SQL.
 */
export interface GeradorParams {
  /** ID do portfólio de destino */
  destino: number;
  /** Quantidade de ações a criar (padrão: 5) */
  quantidadeAcoes?: number;
  /** Quantidade mínima de etapas por ação (padrão: 2) */
  minEtapas?: number;
  /** Quantidade máxima de etapas por ação (padrão: 5) */
  maxEtapas?: number;
  /** Valores fixos que podem ser sobrescritos */
  valoresFixos?: {
    stAcoEtt?: number;
    stEtpAco?: number;
    pcReaTot?: number;
    stRep?: number;
  };
}

/**
 * Informações de uma coluna de tabela.
 */
export interface ColunaInfo {
  /** Nome da coluna */
  nome: string;
  /** Tipo da coluna (ex.: INT, VARCHAR) */
  tipo: string;
}

/**
 * Informações de uma tabela.
 */
export interface TabelaInfo {
  /** Nome da tabela */
  nome: string;
  /** Lista de colunas */
  colunas: ColunaInfo[];
}
```

### 3. Criação de Templates SQL

Crie arquivos de template para cada parte do script SQL. Por exemplo, `src/templates/acao.ts`:

```typescript
/**
 * Gera o INSERT para uma ação (T696ACES).
 *
 * @param destino - ID do portfólio de destino
 * @param numeroAcao - Número sequencial da ação (1..N)
 * @param valoresFixos - Valores fixos opcionais
 * @returns String SQL do INSERT
 */
export function gerarInsertAcao(
  destino: number,
  numeroAcao: number,
  valoresFixos?: { stAcoEtt?: number },
): string {
  const stAcoEtt = valoresFixos?.stAcoEtt ?? 10;
  const nomeAcao = `Portifolio ${destino} - Ação Estratégica ${String(numeroAcao).padStart(2, '0')}`;
  return `
    INSERT INTO [dbo].[T696ACES] 
        ([NM_ACO_ETT], [DE_ACO_ETT], [SQ_ORI_ACO], [CD_UND], [DH_LIM], [MT_USU_ALT], [NM_USU_ALT], [DH_ALT], [ST_ACO_ETT], [DE_HST], [MT_USU_RSP], [CD_UND_APR], [SQ_PTF_ACO], [TP_ACO_ETT])
    VALUES 
        ('${nomeAcao}', NULL, NULL, NULL, NULL, NULL, NULL, GETDATE(), ${stAcoEtt}, NULL, NULL, NULL, ${destino}, NULL);
  `;
}
```

### 4. Funções Utilitárias

Crie `src/utils/sql.ts` com funções para montar as partes do script:

```typescript
/**
 * Gera o cabeçalho do script com os parâmetros de controle.
 *
 * @param destino - ID do portfólio de destino
 * @returns String com o cabeçalho SQL
 */
export function gerarCabecalho(destino: number): string {
  return `
    -- Parâmetros de controle
    DECLARE @SQ_PTF_ACO_DESTINO INT = ${destino};
  `;
}

/**
 * Gera o INSERT para uma etapa (T696ETAC).
 *
 * @param destino - ID do portfólio de destino
 * @param numeroAcao - Número da ação (1..N)
 * @param numeroEtapa - Número da etapa (1..M)
 * @param valoresFixos - Valores fixos opcionais
 * @param anoRealizacao - Ano de realização (AA_REA)
 * @returns String SQL do INSERT
 */
export function gerarInsertEtapa(
  destino: number,
  numeroAcao: number,
  numeroEtapa: number,
  valoresFixos?: { stEtpAco?: number; pcReaTot?: number; stRep?: number },
  anoRealizacao?: number,
): string {
  const stEtpAco = valoresFixos?.stEtpAco ?? 1;
  const pcReaTot = valoresFixos?.pcReaTot ?? 0;
  const stRep = valoresFixos?.stRep ?? 0;
  const nomeEtapa = `Portfolio ${destino} - Ação Estratégica ${String(numeroAcao).padStart(2, '0')} - Etapa ${String(numeroEtapa).padStart(2, '0')}`;
  const aaRea = anoRealizacao ?? new Date().getFullYear();
  return `
    INSERT INTO [dbo].[T696ETAC] 
        ([SQ_ACO_ETT], [CD_UND], [DE_ETP_ACO], [DE_ENT_ETP_ACO], [DH_ALT], [MT_USU_ALT], [NM_USU_ALT], [ST_ETP_ACO], [AA_REA], [PC_REA_TOT], [ST_REP])
    VALUES 
        (@NewAcoID, NULL, '${nomeEtapa}', NULL, GETDATE(), NULL, NULL, ${stEtpAco}, ${aaRea}, ${pcReaTot}, ${stRep});
  `;
}
```

### 5. Lógica Principal (index.ts)

Crie `src/index.ts` que:

- Lê os parâmetros de entrada (via linha de comando ou arquivo de configuração)
- Valida os parâmetros
- Gera o script SQL completo concatenando os templates
- Salva o resultado em um arquivo `.sql` no diretório `output/`

Exemplo de estrutura:

```typescript
import { gerarCabecalho, gerarInsertEtapa } from './utils/sql';
import { gerarInsertAcao } from './templates/acao';
import { GeradorParams } from './types';

/**
 * Gera o script SQL completo.
 *
 * @param params - Parâmetros de entrada
 * @returns String com o script SQL
 */
function gerarScript(params: GeradorParams): string {
  const cabecalho = gerarCabecalho(params.destino);
  const quantidadeAcoes = params.quantidadeAcoes ?? 5;
  const minEtapas = params.minEtapas ?? 2;
  const maxEtapas = params.maxEtapas ?? 5;

  let script = cabecalho;
  script += '\n\n-- Início da criação de ações e etapas\n';

  for (let i = 1; i <= quantidadeAcoes; i++) {
    script += `\n-- Ação ${i}\n`;
    script += gerarInsertAcao(params.destino, i, params.valoresFixos);
    script += '\nSET @NewAcoID = SCOPE_IDENTITY();\n';

    // Para a primeira ação, garantir pelo menos 2 etapas com anos específicos
    if (i === 1) {
      // Etapa 1 - ano corrente (2026)
      script += gerarInsertEtapa(
        params.destino,
        i,
        1,
        params.valoresFixos,
        new Date().getFullYear(), // 2026
      );
      // Etapa 2 - ano subsequente (2027)
      script += gerarInsertEtapa(
        params.destino,
        i,
        2,
        params.valoresFixos,
        new Date().getFullYear() + 1, // 2027
      );
      // Se houver mais etapas (3..maxEtapas), gerar com anos aleatórios
      const numEtapasAdicionais = Math.floor(Math.random() * (maxEtapas - 2 + 1)) + 2; // 2..maxEtapas
      for (let j = 3; j <= numEtapasAdicionais; j++) {
        script += gerarInsertEtapa(
          params.destino,
          i,
          j,
          params.valoresFixos,
          new Date().getFullYear() + Math.floor(Math.random() * 3), // 2026..2028
        );
      }
    } else {
      // Demais ações: gerar quantidade aleatória de etapas (2..5)
      const numEtapas = Math.floor(Math.random() * (maxEtapas - minEtapas + 1)) + minEtapas;
      for (let j = 1; j <= numEtapas; j++) {
        script += gerarInsertEtapa(
          params.destino,
          i,
          j,
          params.valoresFixos,
          new Date().getFullYear() + Math.floor(Math.random() * 3), // 2026..2028
        );
      }
    }
  }

  script += '\nPRINT \'Carga de massa de dados realizada.\';\n';
  return script;
}

// Leitura de argumentos da linha de comando
const args = process.argv.slice(2);
const destino = parseInt(args[0], 10);
const quantidadeAcoes = args[1] ? parseInt(args[1], 10) : 5;

if (isNaN(destino)) {
  console.error('Uso: node dist/index.js <destino> [quantidadeAcoes]');
  process.exit(1);
}

const script = gerarScript({ destino, quantidadeAcoes });
// Salvar em arquivo
```

### 6. Compilação e Execução

- Compile o TypeScript:
  ```bash
  npx tsc
  ```
- Execute o script gerado:
  ```bash
  node dist/index.js 99 5
  ```
- O arquivo SQL gerado será salvo em `gerador/output/`.

### 7. Testes e Validação

- Crie testes unitários para as funções de geração de SQL usando Vitest.
- Nomeie os testes como `deve[Acao][Condicao]` (ex.: `deveGerarCabecalhoComParametrosCorretos`).
- Estruture os testes em `tests/` espelhando `src/`.
- Valide que o script gerado é sintaticamente correto executando-o em um ambiente de teste (ex.: SQL Server local).

## Considerações Finais

- O script deve ser flexível para permitir alterações nos valores fixos (ex.: `ST_ACO_ETT = 10`) sem necessidade de recompilação.
- Considere adicionar suporte a parâmetros adicionais, como nomes de tabelas ou colunas, para maior reutilização.
- Documente o uso do script no próprio arquivo `README.md` dentro da pasta `gerador`.
- Respeite as regras de formatação, nomenclatura, JSDoc, testes, SOLID e camadas descritas acima.

### Regra Específica para a Primeira Ação

- A **primeira ação** (ação 01) deve ter **pelo menos 2 etapas**.
- A **etapa 1** deve ter `AA_REA` igual ao **ano corrente** (ex.: 2026).
- A **etapa 2** deve ter `AA_REA` igual ao **ano subsequente** (ex.: 2027).
- As demais etapas (se houver) podem ter anos aleatórios entre 2026 e 2028.
- As demais ações (02 a 05) podem ter quantidade aleatória de etapas (2 a 5) com anos aleatórios entre 2026 e 2028.

## Próximos Passos

1. Implementar a estrutura básica descrita acima.
2. Testar a geração de um script SQL que cria 5 ações com 2 a 5 etapas cada.
3. Adicionar validações e tratamento de erros.
4. (Opcional) Criar uma interface de linha de comando mais amigável (ex.: usando `commander`).
