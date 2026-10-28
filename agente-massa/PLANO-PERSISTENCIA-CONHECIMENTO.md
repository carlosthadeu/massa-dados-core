# Plano de persistência do conhecimento de domínio

**Status:** rascunho para edição e consenso
**Armazenamento aprovado:** PostgreSQL + JSONB + busca textual + pgvector (pgvector disponível)
**Escopo:** carregar em lote o conhecimento do domínio (S696 e correlatos) em um repositório persistente, para que o agente o use ao **gerar scripts SQL de carga de dados** a partir de comandos em linguagem natural.

> Como editar: ajuste os textos livremente e marque as decisões da seção 10. Itens marcados com **(hipótese)** são sugestões minhas, ainda não validadas.

## 1. O que o agente faz (e o que não faz)

O agente **não responde perguntas sobre o domínio**. Ele recebe um **comando** e produz um **script SQL** de carga coerente com as regras do domínio. Exemplo:

> "Gere uma ação que foi rejeitada pela diretoria, com duas etapas"

Para cumprir esse comando, o agente precisa saber, a partir do conhecimento persistido:

- o que "rejeitada pela diretoria" significa **nos dados** (quais tabelas, quais valores de status/situação, quais registros de histórico, em que ordem, com quais datas e responsáveis);
- que restrições valem (estados e transições válidos, colunas obrigatórias, chaves e dependências entre tabelas);
- como é um dado real equivalente (exemplo de referência), para clonar e adaptar.

## 2. Princípios

1. **Carga offline, sem chat.** O conhecimento entra por uma ferramenta de ingestão executada por linha de comando, a partir de arquivos de origem versionados. O chat serve depois, para refinar.
2. **A ferramenta é o produto.** O que se constrói aqui é o mecanismo de carga, não o conteúdo. Os documentos de amostra (`aprendizado.md`, `analise-diretoria-csv.md`) servem para desenvolver e testar a ferramenta; o conteúdo real chega depois, aos poucos.
3. **Carga incremental e tolerante a lacunas.** Cada documento pode ser carregado sozinho, em qualquer ordem e quantas vezes for preciso. Informação ausente não impede a carga: o item entra como `pendente` e o relatório diz exatamente o que falta e onde. Quando a informação chega, uma nova carga completa o item sem refazer o resto.
4. **Recuperação, não carregamento.** A cada comando, o agente recebe somente o conhecimento relevante àquele comando (poucas receitas, regras e exemplos), dentro de um orçamento de tokens. Nunca a base inteira nem arquivos completos.
5. **Consulta guiada pelo comando, não por perguntas.** Não existem consultas do tipo "quais são os estados" ou "quais transições saem do estado X". A busca parte do texto do comando e devolve o material necessário para gerar o script.
6. **Geração determinística sempre que possível.** O conhecimento deve ser rico o bastante para que o gerador aplique o padrão, sem que o modelo invente valores. Onde o comando for ambíguo ou o conhecimento estiver pendente, o agente pede esclarecimento ou avisa o que falta.
7. **Ingestão idempotente e rastreável.** Reexecutar a carga não duplica dados; cada item guarda origem, versão e data.
8. **Sem execução de SQL de negócio.** O script é gerado e entregue para revisão; o agente não o executa.
9. **Fonte da verdade explícita.** O conhecimento carregado é a referência; a memória conversacional do Mastra serve apenas ao contexto da conversa.

## 3. Situação atual (resumo)

- [aprendizado.md](../agente-s696/project-docs/aprendizado.md) é só uma **amostra** do conhecimento: estados do portfolio (com códigos ainda indefinidos) e um diagrama Mermaid de estados da proposta de ação. O volume real é muito maior.
- Os **exemplos reais virão em arquivos CSV**, como o de [cvs-exemplo](../agente-s696/project-docs/cvs-exemplo/Portfolio%20analise%20diretoria.txt): linhas de tabelas (por exemplo, `SQ_PTF_ACO`, `NM_PTF`, `ST_PTF_ACO`) acompanhadas de um histórico textual (`DE_HST`) com ações, datas e responsáveis.
- Observação sobre a amostra: o arquivo está em UTF-8 válido. A corrupção ("AÃ§Ã£o") só aparece quando ele é lido com a codificação errada (por exemplo, em algumas ferramentas do Windows). A ingestão deve exigir UTF-8 e validá-la.
- `agente-s696` já tem infraestrutura PostgreSQL (pool, TLS validado, schema `adm`, repositório de templates) e geração de preview/SQL. `agente-massa` usa LibSQL apenas para a memória/armazenamento do Mastra.
- Não existe hoje nenhuma persistência de conhecimento de domínio.

## 4. Tipos de conhecimento

| Tipo | Papel na geração | Origem | Tratamento |
|---|---|---|---|
| **Receita** | Traduz um conceito de negócio em padrão de dados (ex.: "ação rejeitada pela diretoria" → status, tabelas, registros de histórico, ordem) | Escrita/validada por especialista; derivada de exemplos reais | Registro estruturado (JSONB) + descrição textual para busca |
| **Máquina de estados** | Diz o que cada estado significa, como o usuário o chama no comando, como ele aparece nos dados e quais ações levam de um estado a outro | **Documentos `.md`/`.txt` de máquina de estados** (ver 4.1), como `aprendizado.md` | Estados, transições (origem → destino, ação) e mapeamento para colunas/valores, em tabelas estruturadas + texto para busca |
| **Regra / restrição** | Valida e compõe os dados (obrigatoriedades, dependências entre tabelas, restrições além dos estados) | `.md`, DDL | Texto + relações estruturadas |
| **Exemplo real** | Linhas de referência a clonar e adaptar | **Arquivos CSV** | Conjunto de linhas por tabela, ligado a um cenário (ver seção 5) |
| **Esquema** | Significado de tabelas e colunas, tipos, chaves | DDL (`.sql`), `.csv` | Um item por tabela/coluna, com descrição de negócio |
| **Glossário** | Liga linguagem do comando a termos técnicos (ex.: "diretoria" ↔ `AnaliseDiretoria`, siglas, sinônimos) | `.csv`/`.md` | Itens curtos, usados também na busca textual |

### 4.1 Documentos de máquina de estados

O **resumo do estado e seus termos de comando** e a **identificação do estado nos dados** não ficam nos documentos de cenário (seção 5). Eles vêm de **documentos de máquina de estados**, carregados em `.md` e/ou `.txt`, no estilo de [aprendizado.md](../agente-s696/project-docs/aprendizado.md). Há um documento (ou seção) por entidade de negócio que tem ciclo de vida: o portfolio, a proposta/ação estratégica, e outras que surgirem.

Cada documento de máquina de estados fornece:

1. **Estados:** nome canônico, rótulo de negócio e descrição curta ("o que significa e quando ocorre").
2. **Termos de comando:** como o usuário chama o estado (por exemplo, "análise da diretoria", "em análise pela diretoria"). Entram no glossário e na busca.
3. **Identificação nos dados:** tabela, coluna e valor que marcam o estado (por exemplo, `T696ACES.ST_ACO_ETT = 10`).
4. **Transições:** estado de origem → estado de destino, com o **nome da ação**, que é o mesmo texto que aparece em `Ação:` no histórico (`DE_HST`). É isso que liga a máquina de estados ao histórico dos exemplos.
5. **Ligação com o cenário:** qual documento de cenário (seção 5) traz os dados de exemplo daquele estado.

Formato aceito (rascunho): o diagrama Mermaid `stateDiagram-v2` (como já existe no exemplo) continua sendo a fonte das **transições**; uma **tabela markdown** acompanha cada diagrama com os campos que o diagrama não carrega:

```markdown
| Estado (id no diagrama) | Rótulo de negócio | Termos de comando | Resumo | Onde aparece nos dados |
|---|---|---|---|---|
| AnaliseDiretoria | Proposta em Análise - Diretoria Executiva | análise da diretoria, em análise pela diretoria | Proposta aguardando decisão da Diretoria Executiva | T696ACES.ST_ACO_ETT = 10 |
```

O `aprendizado.md` atual é uma **amostra**: não traz todos os códigos, termos de comando nem a ligação com os cenários. Isso **não bloqueia nada**. A ferramenta deve carregar o que existir, marcar o que faltar como pendente e permitir completar depois (princípio 3). Para o desenvolvimento da ferramenta, as lacunas da amostra são casos de teste:

| Situação na amostra | Comportamento esperado da ferramenta |
|---|---|
| Estados do portfolio com código `?` ("Em Replanejamento", "Em encerramento") | Carrega os estados; os sem código ficam `pendente`; o relatório lista cada um |
| Estados da proposta no Mermaid, sem código nem coluna de dados | Carrega estados e transições; "onde aparece nos dados" fica pendente |
| Estados sem termos de comando | Carrega; fica pendente para busca por comando até os termos chegarem |
| Cenário que cita uma ação que não existe na máquina de estados | Carrega; divergência no relatório |
| Estado sem transição de "rejeição" (ex.: a partir de `AnaliseDiretoria`) | Carrega o que existe; comando ambíguo é tratado na geração (o agente pergunta), não na carga |
| Nova carga com o mesmo estado, agora completo | Atualiza o item (nova versão), sem duplicar e sem alterar o resto |

Regras de ingestão para esses documentos:

- O Mermaid é interpretado de forma determinística, gerando estados e transições; a tabela de estados é validada contra o diagrama (todo estado do diagrama deve constar na tabela e vice-versa).
- Estados sem código ou sem termos de comando entram como **pendentes** e o gerador recusa usá-los, avisando o que falta.
- Estados e ações citados nos documentos de cenário e no `DE_HST` dos exemplos são conferidos contra a máquina de estados; divergências aparecem no relatório de carga.
- Documentos em `.txt` seguem a mesma estrutura (listas e tabelas simples); o Mermaid só é reconhecido em blocos delimitados.

## 5. Exemplos reais: documento de cenário em markdown

Cada **estado do domínio** é descrito por um documento markdown de cenário, como [analise-diretoria-csv.md](../agente-s696/project-docs/cvs-exemplo/analise-diretoria-csv.md). Não há manifesto separado: o próprio markdown é a fonte e a ingestão o lê por estrutura.

Estrutura observada no modelo:

- Título: `# Conjunto de dados que caracterizam o estado <estado>`.
- Uma seção `##` por tabela (Portfolio, Ação Estratégica, Etapas, Vinculação, Domínio, Entrega, APID).
- Dentro de cada tabela: `### DDL` (bloco `sql`, com chaves primárias e estrangeiras) e `### CSV` (bloco com cabeçalho e linhas reais).
- Colunas de histórico textual (`DE_HST`) com blocos "Em / Por / Ação / Descrição / Notas".

A ingestão:

1. Divide o documento por título e identifica tabela, DDL e CSV de cada seção.
2. Extrai do DDL colunas, tipos, obrigatoriedade, chaves primárias e estrangeiras, o que permite montar a **ordem de inserção** e a ligação entre tabelas.
3. Lê cada bloco CSV (separador, aspas e campos com várias linhas) e valida colunas contra o DDL.
4. Interpreta o `DE_HST` em eventos estruturados (data, autor, ação, descrição), identificando **a sequência de ações que leva ao estado**.
5. Gera uma **receita candidata** (padrão extraído do exemplo) para revisão humana, em vez de depender só da memória do modelo.
6. Mantém nomes e matrículas como estão: são dados fictícios de servidor de teste (decisão 10.6).

### O que o markdown ainda precisa dizer

O modelo atual traz DDL e dados, mas não responde a perguntas que o gerador precisa. Sugestão de seções adicionais por documento, em prosa curta (ver a resposta no chat para o detalhamento):

O resumo do estado, os termos de comando e a identificação do estado nos dados **não** são responsabilidade do documento de cenário: vêm dos documentos de máquina de estados (seção 4.1). O cenário deve apenas indicar qual estado ilustra (pelo nome canônico). O que falta no cenário:

1. **Papéis de cada tabela:** raiz, filha, apoio; relação entre elas e cardinalidade típica (quantas etapas por ação etc.).
2. **Colunas fixas × variáveis:** o que o gerador pode variar (nomes, datas, quantidades) e o que deve permanecer constante.
3. **Dependências externas:** tabelas de referência que o exemplo apenas referencia (`T696PLEM`, `T696VARI`, `T696ORAC`, `T696ORES`, `T696DOMI`) e como obter valores válidos para elas.
4. **Datas do histórico:** como derivar as datas e os intervalos entre as ações (a ordem das ações vem da máquina de estados).
5. **Dados pessoais:** quais colunas e trechos do histórico são pessoais.
6. **Pendências:** lacunas explícitas, para entrarem como itens `pendente`.

### 5.1 Tratamento de históricos longos

O histórico (`DE_HST`) não é essencial para o gerador, e na amostra ele chega a dezenas de eventos de replanejamento repetidos. Regra decidida: **se o histórico ficar longo, ele é cortado na ingestão.**

- O histórico **não é vetorizado** nem entra na busca; ele só acompanha as linhas de exemplo.
- O corte é feito por **eventos inteiros** (blocos "Em / Por / Ação"), nunca no meio de um bloco.
- O limite é configurável (número de eventos ou tamanho em caracteres). Valor inicial **(hipótese)**: manter o evento de criação e os últimos N eventos, que são os que levam ao estado; N a definir na Fase 3 com os exemplos reais.
- O relatório de carga registra quantos eventos foram descartados em cada linha, para que o corte seja visível e não silencioso.
- A conferência das ações contra a máquina de estados considera os eventos mantidos.

Consequência a validar: ao clonar um exemplo, o histórico gerado terá o mesmo formato, mas poderá ter menos eventos que o original, desde que a sequência final seja coerente com a máquina de estados.

## 6. Armazenamento (decidido)

**PostgreSQL + JSONB + busca textual + pgvector**, em schema próprio (por exemplo `conhecimento`), separado do schema `adm`.

Motivos: o S696 já usa PostgreSQL; um único banco cobre transações, versionamento, auditoria e busca híbrida em SQL; sem serviço novo. Relações entre tabelas e transições de estado ficam numa tabela de arestas comum, sem banco de grafos.

| Recurso | Uso |
|---|---|
| JSONB | Receitas, regras estruturadas e linhas dos exemplos (campos que variam por tabela) |
| Busca textual (`tsvector`, configuração `portuguese`) + sinônimos | Palavras-chave, siglas e nomes técnicos do comando |
| pgvector (índice HNSW, distância de cosseno) | Similaridade semântica entre o comando e as descrições de receitas, regras e cenários |
| Busca híbrida | Combina os dois rankings (por exemplo, Reciprocal Rank Fusion) e filtra por tipo e status |

**Ambiente de destino (decidido):** Aiven for PostgreSQL, plano gratuito. Limites da [documentação](https://aiven.io/docs/products/postgresql/concepts/pg-free-tier): 1 CPU, 1 GB de RAM, **1 GB de disco**, `max_connections` = 20, sem pool de conexões; há suporte a extensões; o serviço pode ser desligado por inatividade (com aviso prévio) e religado depois. Consequências: o volume pequeno cabe com folga, mas é preciso medir o uso do disco (JSONB das linhas dos exemplos e vetores); a ferramenta deve usar poucas conexões e fechar o pool ao terminar; falhas de conexão com o serviço desligado devem dar mensagem clara. O modelo de embeddings roda fora do banco, então o limite de 1 GB de RAM não o afeta. Pendências técnicas: **(resolvidas, segundo a documentação da Aiven)** a extensão `vector` (pgvector) está disponível no Aiven for PostgreSQL, na versão 0.8.6 (confirmar na versão do PostgreSQL do serviço), e todo usuário de banco, inclusive `avnadmin`, pode executar `CREATE EXTENSION`, sem privilégios elevados. Conforme a [documentação da Aiven](https://aiven.io/docs/products/postgresql/howto/use-pgvector), a extensão é habilitada uma vez por banco (`CREATE EXTENSION vector;`), a coluna é `vector(N)`, a distância por cosseno usa o operador `<=>` e o índice aproximado (ivfflat) é opcional: com volume pequeno, a busca exata basta. A migração deve rodar `CREATE EXTENSION IF NOT EXISTS vector` e falhar com mensagem clara se faltar permissão; a dimensão do vetor (384) vem do modelo `multilingual-e5-small` (decisão 10.3). Embeddings são gerados localmente; nenhum conteúdo é enviado a serviço externo.

Nota de projeto: **o que se vetoriza são as descrições** (receitas, regras, cenários, glossário), não as linhas brutas dos CSVs. As linhas ficam em JSONB, ligadas ao cenário, e são recuperadas por identificador depois que o cenário é escolhido.

## 7. Modelo de dados proposto (rascunho)

> **Implementado nas Fases 1 e 2** (`agente-s696/src/knowledge/schema.ts`). Diferenças em relação ao rascunho abaixo: tabelas físicas com prefixo `conhecimento_` no schema do `.env`; `tipo` inclui `estado`; `conhecimento_relacao` e `conhecimento_sinonimo` referenciam chaves lógicas e a fonte (são substituídas a cada carga da fonte), pois destinos podem ainda não existir; versões antigas de um item ficam com `vigente = false` e perdem os trechos (limite de 1 GB do plano gratuito); `exemplo_linha` e a coluna `embedding` virão nas Fases 3 e 4. CLI disponível: `migrar`, `carregar`, `pendencias`, `listar-cargas`, `verificar`; `exportar`, `aprovar` e `reindexar` ficam para as próximas fases. Diretiva `Entidade: X` (ou um título) nomeia a entidade de uma máquina de estados; sem ela vale a linha de contexto do bloco.

```text
conhecimento.fonte         (fonte_id, caminho, hash, tipo, codificacao, carga_id)
conhecimento.item          (item_id, tipo, chave_logica, titulo, descricao, estruturado JSONB,
                            versao, status, hash_conteudo, criado_em)
                            -- tipo: receita | regra | cenario | esquema | glossario
conhecimento.trecho        (trecho_id, item_id, ordem, texto, tokens, tsv TSVECTOR,
                            embedding VECTOR(384), modelo_embedding)
conhecimento.relacao       (origem_item_id, destino_item_id, tipo, rotulo, estruturado JSONB)
                            -- transição de estado, dependência/chave estrangeira, receita→cenário
conhecimento.exemplo_linha (exemplo_id, item_id, tabela_destino, chave_origem, valores JSONB, ordem)
conhecimento.sinonimo      (termo, equivalente)
conhecimento.carga         (carga_id, iniciada_em, concluida_em, executor, resumo, erros JSONB)
```

Regras principais:

- `chave_logica` identifica o item entre cargas; chave + `hash_conteudo` permitem upsert idempotente.
- Alterações geram nova `versao`; versões anteriores ficam inativas, não são apagadas.
- Itens com lacunas (por exemplo, códigos de estado ainda "?") entram como `status = pendente`, nunca preenchidos por suposição; o gerador recusa usar receita pendente e avisa.
- Receita gerada automaticamente a partir de CSV entra como `candidata` até ser aprovada.
- Toda recuperação devolve `item_id`/versão, para rastrear de onde veio cada valor do script.

## 8. Pipeline de ingestão

```text
arquivos de origem (documentos md/txt de máquina de estados, documentos md de cenário, ddl)
  → detectar tipo e codificação
  → parser por tipo (máquina de estados: tabela de estados + Mermaid; cenário: DDL, CSV, histórico DE_HST)
  → conferir cenário e histórico contra a máquina de estados (estados, ações, códigos)
  → normalizar em itens, relações e linhas de exemplo
  → validar (colunas × esquema, chaves, referências, duplicidades, pendências)
  → derivar receitas candidatas a partir dos exemplos
  → dividir descrições em trechos e gerar embeddings com modelo local (lote, cache por hash; o histórico não é vetorizado)
  → gravar em transação (upsert por chave_logica + hash)
  → relatório da carga (novos, alterados, inalterados, rejeitados, pendentes)
```

Interface de linha de comando (rascunho):

```text
conhecimento carregar <diretório|arquivo> [--dry-run] [--tipo ...] [--forcar]
conhecimento pendencias [--estado ...] # lista o que falta completar, por documento e item
conhecimento listar-cargas
conhecimento verificar         # integridade: referências, trechos sem embedding, receitas pendentes
conhecimento exportar <chave>  # leitura para revisão
conhecimento aprovar <chave>   # promove receita candidata a ativa
conhecimento reindexar         # recalcula embeddings (troca de modelo)
```

O relatório de cada carga (e o comando `pendencias`) devolve uma lista acionável, por exemplo: "estado `AnaliseDiretoria`: falta código em `T696ACES.ST_ACO_ETT`; sem termos de comando; sem cenário associado". Assim cada nova carga nasce do que o relatório anterior apontou.

## 9. Plano de execução

### Fase 0 — Decisões e inventário
- Fechar as decisões da seção 10.
- Inventariar as fontes (formatos, volume, quem mantém) e os **comandos típicos** que o agente precisará atender.
- Montar o conjunto de **comandos de referência** com o resultado esperado (quais tabelas/valores/registros), validado por especialista. É o teste de aceite da seção 11. Pode crescer à medida que o conhecimento for carregado.

**Resultado:** decisões registradas, fontes listadas e casos de referência iniciais.

> **Status:** Fases 1 e 2 implementadas e testadas com PGlite (Postgres em memória); ainda não executadas no Aiven real.

### Fase 1 — Fundação de armazenamento
- Criar schema e migrações (seção 7).
- Habilitar o pgvector (confirmar versão e permissão) e criar índices (GIN para `tsvector`, HNSW para vetores).
- Conectar com TLS validado, seguindo o padrão de `agente-s696/src/config.ts`.
- Definir onde o código fica (decisão 10.4).

**Resultado:** esquema criado e testado em banco descartável.

### Fase 2 — Ingestão de máquinas de estados e esquema
- Parser dos documentos `.md`/`.txt` de máquina de estados (seção 4.1): tabela de estados (rótulo, termos de comando, resumo, tabela/coluna/valor) e Mermaid (estados e transições com ação).
- Validação cruzada entre tabela e diagrama; itens incompletos entram como pendentes.
- Carga dos termos de comando no glossário e na busca.
- Parser de DDL.
- Validação, divisão em trechos e gravação idempotente, com atualização incremental (reexecutar um documento completa itens pendentes sem tocar nos demais).
- CLI com `--dry-run`, relatório de carga e comando `pendencias`.
- Testes: use os documentos de amostra como fixtures, incluindo as lacunas da seção 4.1; reexecução sem duplicar, alteração gera nova versão, arquivo inválido, falha no meio da carga, pendência resolvida por nova carga.

**Resultado:** ferramenta de carga utilizável pelos documentos de máquina de estados e pelo esquema, já alimentável aos poucos.

### Fase 3 — Ingestão de exemplos CSV e receitas
- Leitura do documento de cenário (DDL + CSV por tabela), com codificação validada na carga.
- Validação contra o DDL e resolução de chaves e ordem de inserção entre tabelas.
- Interpretação do histórico `DE_HST` em eventos estruturados, com truncamento por eventos inteiros (seção 5.1), conferindo as ações mantidas contra as transições da máquina de estados.
- Geração de receitas candidatas (estado + transições + exemplo) e fluxo de aprovação (`aprovar`).
- Dados pessoais: nenhum tratamento especial (decisão 10.6, dados fictícios).

**Resultado:** cenários e receitas carregados, com receitas candidatas revisáveis.

### Fase 4 — Indexação e recuperação
- Embeddings em lote com **modelo local**, com cache, retentativa e registro do modelo.
- Busca híbrida a partir do texto do comando, devolvendo: receita(s) aplicável(is) + regras relevantes + cenário de referência, dentro do orçamento de tokens configurável.
- Limiar mínimo de relevância; abaixo dele, resposta explícita de "não encontrado".
- Medição com os comandos de referência da Fase 0.

**Resultado:** biblioteca de recuperação com qualidade medida.

### Fase 5 — Integração com o agente e o gerador
- Ferramenta Mastra **somente de leitura** que, dado o comando, devolve o pacote de conhecimento necessário (registrada em `agente-massa/src/mastra/tools/` e `src/mastra/index.ts`).
- Integrar com a geração de preview/SQL já existente no `agente-s696`: o conhecimento fornece o padrão e os valores; o gerador monta o script.
- Instruções do agente: usar somente o que a recuperação devolveu, pedir esclarecimento quando houver ambiguidade (por exemplo, a quantidade de etapas ou qual diretoria), recusar receita pendente, não inventar valores.
- Antes de implementar, consultar a documentação embarcada do Mastra instalado (conforme `AGENTS.md`).

**Resultado:** o comando de exemplo gera um script revisável, apoiado na carga.

### Fase 6 — Refinamento pela conversa
- O agente poderá **propor** correções ou novas receitas; a proposta fica registrada e só vira versão ativa com aprovação humana.
- O agente não grava diretamente no conhecimento publicado.

**Resultado:** refinamento controlado e auditável.

### Fase 7 — Operação e documentação
- Documentar variáveis de ambiente, procedimento de carga, atualização, backup, verificação e retorno a versão anterior.

## 10. Decisões pendentes

1. ~~**Armazenamento**~~ **Decidido:** PostgreSQL + JSONB + busca textual + pgvector (disponível).
2. ~~**Uso do conhecimento**~~ **Decidido:** combinação de receitas, regras/restrições e exemplos reais, para gerar o script de carga.
3. ~~**Modelo de embeddings**~~ **Decidido:** sem serviço externo; os embeddings são gerados por um **modelo local**, executado na própria máquina/servidor, sem enviar conteúdo a terceiros. **Modelo inicial escolhido: `intfloat/multilingual-e5-small`** (multilíngue, inclui português; 384 dimensões; licença MIT; roda em CPU; contexto de 512 tokens, suficiente para trechos curtos). A coluna passa a ser `VECTOR(384)`. Observação de uso: este modelo exige prefixos `passage: ` na indexação e `query: ` na consulta. Alternativa se a qualidade em português for insuficiente: `BAAI/bge-m3` (1024 dimensões, mais pesado). Trocar de modelo exige `reindexar`. Validação final na Fase 4 com os comandos de referência.
   - Modelo local escolhido: ________
4. ~~**Onde fica o código**~~ **Decidido:** todo o código de conhecimento (tabelas `conhecimento_*` no schema do S696, parsers, CLI de carga, recuperação) fica em `agente-s696`. O `agente-massa` apenas fornece a interface de chat ao S696 e não contém lógica de conhecimento.
   - Resposta: ________
5. ~~**Rotulagem dos CSVs**~~ **Decidido:** um documento markdown por estado, no modelo de `analise-diretoria-csv.md`, sem manifesto separado. Pendente: quem escreve os documentos e quais seções adicionais serão obrigatórias (seção 5).
   - Resposta: ________
6. ~~**Dados pessoais nos exemplos**~~ **Decidido:** os dados dos CSVs são fictícios, vindos de servidor de teste. São mantidos como estão, sem anonimização nem substituição. Premissa: só entram na carga dados de servidor de teste; a ferramenta não valida isso.
   - Resposta: ________
7. ~~**Receitas**~~ **Decidido (parcial):** as duas origens valem. O modelo (LLM) deriva receitas candidatas a partir da máquina de estados e dos exemplos, e é a opção preferida sempre que não houver ambiguidade. Quando houver ambiguidade (por exemplo, "rejeitada pela diretoria" sem transição de rejeição no diagrama), a receita é escrita ou decidida por um especialista. Toda receita derivada entra como candidata e só vira ativa pelo comando `aprovar`. **Decidido:** quem aprova é quem faz a carga; o comando `aprovar` não exige revisão por terceiros, mas registra o executor e a data. **Expectativa de uso:** cerca de 90% do conhecimento já vem escrito nos documentos carregados; a derivação pelo modelo cobre o restante. A incerteza tende a diminuir com as cargas sucessivas, então a ferramenta deve aceitar lacunas e não exigir completude (princípio 3).
   - Resposta: ________
8. ~~**Banco/schema de destino**~~ **Decidido:** o mesmo banco e o mesmo schema do S696, configurados no `.env` (`DATABASE_URL`, `DATABASE_SCHEMA=adm`). Não há instância nem schema próprio. Para não colidir com as tabelas existentes, as tabelas físicas levam o prefixo `conhecimento_` (por exemplo `adm.conhecimento_item`); os nomes `conhecimento.*` da seção 7 são nomes lógicos. A extensão `vector` precisa existir nesse banco. **Decidido:** por ora só roda em ambiente de teste; ambientes adicionais não são preocupação agora.
   - Resposta: ________
9. ~~**Fontes e volume**~~ **Decidido:** o volume é pequeno. O formato de entrada é o markdown de cenário, que sempre traz o DDL e o CSV de cada tabela (seção 5). Ao recarregar um documento, a ferramenta compara a estrutura (DDL) com a versão já persistida: se mudou, grava a nova versão da estrutura e registra a diferença (colunas incluídas, removidas ou alteradas) no relatório da carga; se não mudou, só atualiza os dados que mudaram (upsert por `chave_logica` + `hash_conteudo`). **Decidido:** quem mantém os documentos não importa por ora (há um único usuário), e a frequência de mudança não é controlada, então a recarga com comparação de estrutura deve funcionar a qualquer momento.
   - Resposta: ________
10. ~~**Codificação**~~ **Resolvida:** a amostra é UTF-8 válido; a ingestão exige UTF-8 e valida na carga.
11. ~~**Origem do resumo, termos de comando e identificação do estado**~~ **Decidido:** documentos de máquina de estados em `.md`/`.txt` (seção 4.1), carregados aos poucos; o conteúdo completo não é pré-requisito para construir a ferramenta. Pendente apenas:
    - Formato final da tabela de estados que acompanha o Mermaid (a validar com a Fase 2).
    - Resposta: ________

## 11. Critérios de aceite

- Cada comando de referência (Fase 0) recupera a receita, as regras e o cenário esperados, dentro do orçamento de tokens.
- Os documentos de amostra carregam sem erro; as lacunas aparecem como pendências no relatório, e uma segunda carga com a informação completa resolve a pendência sem duplicar nada.
- O comando "Gere uma ação rejeitada pela diretoria, com duas etapas" produz um script que um especialista valida como consistente (chaves, estados, histórico, datas).
- Comandos sem receita aplicável ou com receita pendente resultam em aviso claro, não em script inventado.
- Reexecutar a mesma carga não altera a base; alterar uma fonte cria nova versão sem perder a anterior.
- Toda decisão do script é rastreável a itens e versões do conhecimento.
- Nenhuma credencial no código, TLS validado e nenhuma execução automática de SQL de negócio.

## 12. Riscos

| Risco | Mitigação |
|---|---|
| CSV não diz o que representa; o exemplo não é encontrado ou é mal interpretado | Seções de resumo e identificação do estado no documento de cenário, revisão humana das receitas candidatas, casos de referência |
| Receita extraída de um único exemplo generaliza mal | Marcar como candidata, exigir aprovação, preferir mais de um exemplo por conceito |
| Códigos e estados ainda indefinidos ("?") | Status pendente; o gerador recusa e avisa |
| Máquina de estados e exemplos divergem (nome de ação, estado ou código) | Conferência cruzada na carga, com divergências no relatório |
| Comando ambíguo em relação às transições (ex.: "rejeitada pela diretoria") | Termos de comando mapeados a transições; se houver mais de uma, o agente pergunta |
| Dado real entrar por engano na carga | Decisão 10.6: premissa de que só há dados de servidor de teste; conferir a origem dos CSVs antes de carregar |
| Leitura com codificação errada gera texto corrompido | UTF-8 exigido e validado na ingestão |
| Troca futura do modelo de embeddings | Guardar modelo e dimensão por trecho; comando `reindexar` |
| Modelo local com qualidade inferior em português ou lento em CPU | Validar com os comandos de referência na Fase 4; busca híbrida mantém a busca textual como apoio; modelo trocável via `reindexar` |
| Histórico (`DE_HST`) muito longo estoura o orçamento de tokens | Truncar na ingestão (seção 5.1); o histórico não é vetorizado |
| Recuperação traz material irrelevante e o gerador o usa | Busca híbrida, limiar de relevância, medição com casos de referência |
| Estouro de contexto | Orçamento de tokens; só descrições são vetorizadas; linhas dos exemplos recuperadas por identificador |
| APIs do Mastra diferentes do que o modelo conhece | Validar na documentação embarcada da versão instalada |

## 13. Fora de escopo neste momento

- Responder perguntas sobre o domínio (por exemplo, "quais são os estados?").
- Carregar o conhecimento por conversa ou anexando arquivos `.md` ao contexto.
- Execução automática de SQL em bancos de negócio.
- Aprendizado e publicação automáticos sem aprovação.
