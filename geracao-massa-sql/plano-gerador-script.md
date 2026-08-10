# Plano para Criação de Script TypeScript Gerador de Scripts SQL

## Objetivo

Criar um script TypeScript que, a partir de parâmetros de entrada (portfólio de origem, portfólio de destino, etc.), gere automaticamente um script SQL de clonagem de portfólio, similar ao arquivo `clonar portfolio S696.sql`.

## Pré-requisitos

- Node.js instalado (versão 18 ou superior)
- TypeScript instalado globalmente ou como dependência do projeto
- Conhecimento básico de SQL e do esquema das tabelas envolvidas

## Estrutura de Arquivos Sugerida

```
geracao-massa-sql/
├── clonar portfolio S696.sql          (script SQL existente)
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
│   │   │   └── entrega.ts             (template para inserção de entregas)
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
  /** ID do portfólio de origem */
  origem: number;
  /** ID do portfólio de destino */
  destino: number;
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
 * @param origem - ID do portfólio de origem
 * @param destino - ID do portfólio de destino
 * @param colunas - Lista de colunas a inserir
 * @param valores - Lista de valores correspondentes
 * @returns String SQL do INSERT
 */
export function gerarInsertAcao(
  origem: number,
  destino: number,
  colunas: string[],
  valores: string[],
): string {
  return `
    INSERT INTO [dbo].[T696ACES] 
        (${colunas.join(', ')})
    SELECT 
        ${valores.join(', ')}
    FROM [dbo].[T696ACES] 
    WHERE [SQ_ACO_ETT] = @OldAcoID;
  `;
}
```

### 4. Funções Utilitárias

Crie `src/utils/sql.ts` com funções para montar as partes do script:

```typescript
/**
 * Gera o cabeçalho do script com os parâmetros de controle.
 *
 * @param origem - ID do portfólio de origem
 * @param destino - ID do portfólio de destino
 * @returns String com o cabeçalho SQL
 */
export function gerarCabecalho(origem: number, destino: number): string {
  return `
    -- Parâmetros de controle
    DECLARE @SQ_PTF_ACO_ORIGEM INT = ${origem};
    DECLARE @SQ_PTF_ACO_DESTINO INT = ${destino};
  `;
}

/**
 * Gera o cursor para percorrer as ações do portfólio de origem.
 *
 * @returns String com o cursor SQL
 */
export function gerarCursorAcoes(): string {
  return `
    DECLARE C_Acoes CURSOR FOR 
    SELECT SQ_ACO_ETT FROM [dbo].[T696ACES] WHERE SQ_PTF_ACO = @SQ_PTF_ACO_ORIGEM;
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
import { gerarCabecalho, gerarCursorAcoes } from './utils/sql';
import { gerarInsertAcao } from './templates/acao';
import { GeradorParams } from './types';

/**
 * Gera o script SQL completo.
 *
 * @param params - Parâmetros de entrada
 * @returns String com o script SQL
 */
function gerarScript(params: GeradorParams): string {
  const cabecalho = gerarCabecalho(params.origem, params.destino);
  const cursor = gerarCursorAcoes();
  // ... montar o restante do script
  return `${cabecalho}\n${cursor}\n...`;
}

// Leitura de argumentos da linha de comando
const args = process.argv.slice(2);
const origem = parseInt(args[0], 10);
const destino = parseInt(args[1], 10);

if (isNaN(origem) || isNaN(destino)) {
  console.error('Uso: node dist/index.js <origem> <destino>');
  process.exit(1);
}

const script = gerarScript({ origem, destino });
// Salvar em arquivo
```

### 6. Compilação e Execução

- Compile o TypeScript:
  ```bash
  npx tsc
  ```
- Execute o script gerado:
  ```bash
  node dist/index.js 56 99
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

## Próximos Passos

1. Implementar a estrutura básica descrita acima.
2. Testar a geração de um script SQL idêntico ao fornecido.
3. Adicionar validações e tratamento de erros.
4. (Opcional) Criar uma interface de linha de comando mais amigável (ex.: usando `commander`).
