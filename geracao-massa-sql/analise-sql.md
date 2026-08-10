# Análise do Script SQL - Clonar Portfólio S696

## Visão Geral

O script clona dados de um portfólio de origem para um portfólio de destino, copiando:

- **Ações** (T696ACES)
- **Vínculos e Domínios** (T696VIAC, T696DOAC)
- **Etapas** (T696ETAC)
- **Entregas** (T696ENAC)

## Parâmetros de Controle

| Parâmetro | Descrição |
|-----------|-----------|
| `@SQ_PTF_ACO_ORIGEM` | ID do portfólio de origem (ex.: 56) |
| `@SQ_PTF_ACO_DESTINO` | ID do portfólio de destino (ex.: 99) |

## Fluxo do Script

1. **Cursor de Ações** (`C_Acoes`): percorre todas as ações do portfólio de origem.
2. Para cada ação:
   - Insere uma nova ação no portfólio de destino com:
     - `ST_ACO_ETT` fixo em **10**
     - `DE_HST` (histórico) limpo (`NULL`)
     - `DH_ALT` definido como `GETDATE()`
   - Obtém o novo ID da ação (`SCOPE_IDENTITY()`).
   - Clona vínculos (`T696VIAC`) e domínios (`T696DOAC`) associados à ação original.
3. **Cursor de Etapas** (`C_Etapas`): para cada ação clonada, percorre as etapas da ação original.
   - Insere uma nova etapa com:
     - `ST_ETP_ACO` fixo em **1**
     - `PC_REA_TOT` fixo em **0**
     - `ST_REP` fixo em **0**
     - `DH_ALT` definido como `GETDATE()`
   - Obtém o novo ID da etapa (`SCOPE_IDENTITY()`).
4. **Clonagem de Entregas** (`T696ENAC`):
   - Para cada etapa clonada, copia as entregas da etapa original.
   - Mantém o valor original de `PC_PLJ_ENT` (planejamento).
   - Limpa `PC_REA_ENT`, `DE_REA_ENT`, `DE_HST` e `NT_REV` (definidos como `NULL`).
   - `DH_ALT` definido como `GETDATE()`.

## Regras Aplicadas

### Valores Fixos

| Campo | Valor Fixo | Justificativa |
|-------|------------|---------------|
| `ST_ACO_ETT` | 10 | Status da ação |
| `ST_ETP_ACO` | 1 | Status da etapa |
| `PC_REA_TOT` | 0 | Progresso total zerado |
| `ST_REP` | 0 | Status de replicação zerado |

### Campos Limpos (NULL)

| Tabela | Campos Limpos |
|--------|---------------|
| `T696ACES` | `DE_HST` |
| `T696ENAC` | `PC_REA_ENT`, `DE_REA_ENT`, `DE_HST`, `NT_REV` |

### Campos Preservados

| Tabela | Campos Preservados |
|--------|--------------------|
| `T696ENAC` | `PC_PLJ_ENT` (planejamento original) |

## Observações Técnicas

- O script utiliza cursores (`DECLARE CURSOR`) para iterar sobre as linhas, o que pode ser ineficiente para grandes volumes de dados.
- `SCOPE_IDENTITY()` é usado para obter o ID recém-inserido, garantindo que o valor seja o último gerado na sessão.
- Não há tratamento de transações (`BEGIN TRAN` / `COMMIT`), portanto, se ocorrer erro no meio do processo, os dados podem ficar parcialmente clonados.
- O script assume que as tabelas e colunas existem conforme os nomes fornecidos.

## Recomendações

1. **Adicionar transação** para garantir atomicidade.
2. **Considerar uso de `MERGE` ou `INSERT ... SELECT` com `OUTPUT`** para evitar cursores e melhorar performance.
3. **Validar a existência do portfólio de destino** antes de iniciar a clonagem.
4. **Adicionar logs** para rastrear o progresso e possíveis erros.

## Conclusão

O script atende ao objetivo de clonar a estrutura de um portfólio, aplicando regras de limpeza e valores fixos conforme especificado. Porém, melhorias de robustez e performance são recomendadas para uso em produção.
