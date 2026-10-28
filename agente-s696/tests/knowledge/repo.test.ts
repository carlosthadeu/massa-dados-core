import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { PGlite } from '@electric-sql/pglite';
import { carregar } from '../../src/knowledge/carga.js';
import { executarConhecimento } from '../../src/knowledge/cli.js';
import {
  RepositorioConhecimento,
  type Executor,
  type Provedor,
} from '../../src/knowledge/repo.js';

const ESTADOS_V1 = [
  '# Proposta',
  '',
  '```mermaid',
  'stateDiagram-v2',
  '    A : Estado A',
  '    B : Estado B',
  '    A --> B : Avançar',
  '```',
  '',
].join('\n');

const TABELA = (termos: string): string =>
  [
    '| Estado (id no diagrama) | Rótulo de negócio | Termos de comando | Resumo | Onde aparece nos dados |',
    '|---|---|---|---|---|',
    `| A | Estado A | ${termos} | Resumo A | T.COL = 1 |`,
    '| B | Estado B | termo b | Resumo B | T.COL = 2 |',
  ].join('\n');

const DDL = (coluna: string): string =>
  [
    '## T001 - Tabela',
    '```sql',
    `CREATE TABLE T001 (ID int NOT NULL, ${coluna} varchar(10) NULL);`,
    '```',
  ].join('\n');

describe('persistência do conhecimento (PGlite)', () => {
  let db: PGlite;
  let repo: RepositorioConhecimento;
  let dir: string;

  const provedor = (): Provedor => ({
    conectar: (fn) => fn(db as unknown as Executor),
  });

  const arquivo = (nome: string, conteudo: string | Buffer): string => {
    const caminho = path.join(dir, nome);
    writeFileSync(caminho, conteudo);
    return caminho;
  };

  const contar = async (sql: string): Promise<number> =>
    Number((await db.query<{ n: number }>(sql)).rows[0].n);

  beforeEach(async () => {
    db = new PGlite();
    await db.exec('CREATE SCHEMA adm');
    repo = new RepositorioConhecimento(provedor(), 'adm');
    dir = mkdtempSync(path.join(os.tmpdir(), 'conhecimento-'));
    await repo.migrar();
  });

  afterEach(async () => {
    await db.close();
    rmSync(dir, { recursive: true, force: true });
  });

  it('migrar é idempotente', async () => {
    const segunda = await repo.migrar();
    expect(segunda.aplicadas).toEqual([]);
    expect(
      await contar('SELECT count(*) AS n FROM adm.conhecimento_migracao'),
    ).toBe(1);
  });

  it('recarga não duplica; alteração cria nova versão e retira a anterior', async () => {
    const f = arquivo('e.md', ESTADOS_V1 + TABELA('termo a'));
    const opcoes = { executor: 'teste' };

    const primeira = await carregar(repo, [f], opcoes);
    expect(primeira.arquivos[0].situacao).toBe('carregado');
    expect(primeira.arquivos[0].plano?.novos).toHaveLength(2);
    const itens = await contar(
      'SELECT count(*) AS n FROM adm.conhecimento_item',
    );

    const segunda = await carregar(repo, [f], opcoes);
    expect(segunda.arquivos[0].situacao).toBe('inalterado');
    const forcada = await carregar(repo, [f], { ...opcoes, forcar: true });
    expect(forcada.arquivos[0].plano?.inalterados).toHaveLength(2);
    expect(
      await contar('SELECT count(*) AS n FROM adm.conhecimento_item'),
    ).toBe(itens);

    writeFileSync(f, ESTADOS_V1 + TABELA('termo a, outro termo'));
    const alterada = await carregar(repo, [f], opcoes);
    expect(alterada.arquivos[0].plano?.atualizados).toHaveLength(1);
    expect(
      await contar(
        `SELECT count(*) AS n FROM adm.conhecimento_item WHERE chave_logica LIKE '%:A' AND versao = 2 AND vigente`,
      ),
    ).toBe(1);
    expect(
      await contar(
        `SELECT count(*) AS n FROM adm.conhecimento_item WHERE chave_logica LIKE '%:A' AND versao = 1 AND NOT vigente`,
      ),
    ).toBe(1);
    expect(
      await contar(
        `SELECT count(*) AS n FROM adm.conhecimento_trecho t JOIN adm.conhecimento_item i ON i.id = t.item_id WHERE NOT i.vigente`,
      ),
    ).toBe(0);
    expect(
      await contar('SELECT count(*) AS n FROM adm.conhecimento_relacao'),
    ).toBe(1);
  });

  it('a pendência some quando uma nova carga completa o estado', async () => {
    const f = arquivo('e.md', ESTADOS_V1);
    await carregar(repo, [f], { executor: 'teste' });
    expect((await repo.listarPendencias()).length).toBe(2);

    writeFileSync(f, ESTADOS_V1 + TABELA('termo a'));
    await carregar(repo, [f], { executor: 'teste' });
    expect(await repo.listarPendencias()).toEqual([]);
  });

  it('rejeita só o arquivo que não é UTF-8, e carrega os demais', async () => {
    const ruim = arquivo('a-ruim.md', Buffer.from([0x41, 0xe7, 0xe3, 0x6f]));
    const bom = arquivo('b-bom.md', DDL('NOME'));
    const rel = await carregar(repo, [ruim, bom], { executor: 'teste' });
    expect(rel.arquivos.map((a) => a.situacao)).toEqual([
      'rejeitado',
      'carregado',
    ]);
    expect(rel.arquivos[0].erro).toContain('UTF-8');
    expect(
      await contar(
        `SELECT count(*) AS n FROM adm.conhecimento_item WHERE tipo = 'esquema'`,
      ),
    ).toBe(1);
  });

  it('falha no meio do documento desfaz só aquele documento', async () => {
    const bom = arquivo('b-bom.md', DDL('NOME'));
    await carregar(repo, [bom], { executor: 'teste' });
    const antes = await contar(
      'SELECT count(*) AS n FROM adm.conhecimento_item',
    );

    let chamadas = 0;
    const quebrado: Provedor = {
      conectar: (fn) =>
        fn({
          query: async (sql, params) => {
            if (
              sql.includes('INSERT INTO "adm".conhecimento_trecho') &&
              ++chamadas === 1
            ) {
              throw new Error('falha simulada');
            }
            return (db as unknown as Executor).query(sql, params);
          },
        }),
    };
    const repoQuebrado = new RepositorioConhecimento(quebrado, 'adm');
    const novo = arquivo('c-novo.md', DDL('OUTRA'));
    const rel = await carregar(repoQuebrado, [novo], { executor: 'teste' });
    expect(rel.arquivos[0]).toMatchObject({
      situacao: 'rejeitado',
      erro: 'falha simulada',
    });
    expect(
      await contar('SELECT count(*) AS n FROM adm.conhecimento_item'),
    ).toBe(antes);
    expect(
      await contar(
        `SELECT count(*) AS n FROM adm.conhecimento_fonte WHERE caminho LIKE '%c-novo%'`,
      ),
    ).toBe(0);
  });

  it('dry-run não grava e mudança de DDL aparece no relatório', async () => {
    const f = arquivo('d.md', DDL('NOME'));
    await carregar(repo, [f], { executor: 'teste' });

    writeFileSync(f, DDL('NOME_COMPLETO'));
    const simulado = await carregar(repo, [f], {
      executor: 'teste',
      dryRun: true,
    });
    expect(simulado.arquivos[0].situacao).toBe('simulado');
    const mudanca = simulado.arquivos[0].plano?.mudancasEstrutura[0];
    expect(mudanca?.colunasIncluidas).toEqual(['NOME_COMPLETO']);
    expect(mudanca?.colunasRemovidas).toEqual(['NOME']);
    expect(
      await contar('SELECT count(*) AS n FROM adm.conhecimento_carga'),
    ).toBe(1);
    expect(
      await contar('SELECT max(versao) AS n FROM adm.conhecimento_item'),
    ).toBe(1);

    await carregar(repo, [f], { executor: 'teste' });
    expect(
      await contar('SELECT max(versao) AS n FROM adm.conhecimento_item'),
    ).toBe(2);
  });

  it('itens que sumiram do documento são reportados e mantidos', async () => {
    const f = arquivo('s.md', ESTADOS_V1 + TABELA('termo a'));
    await carregar(repo, [f], { executor: 'teste' });
    writeFileSync(
      f,
      ESTADOS_V1.replace('    B : Estado B\n', '').replace(
        'A --> B',
        'A --> A',
      ),
    );
    const rel = await carregar(repo, [f], { executor: 'teste' });
    expect(rel.arquivos[0].plano?.ausentes.length).toBeGreaterThan(0);
    expect(
      await contar(
        `SELECT count(*) AS n FROM adm.conhecimento_item WHERE vigente AND chave_logica LIKE '%:B'`,
      ),
    ).toBe(1);
  });

  it('verificar acusa relação sem destino', async () => {
    await db.exec(
      `INSERT INTO adm.conhecimento_fonte (caminho, hash) VALUES ('x', 'h')`,
    );
    await db.exec(
      `INSERT INTO adm.conhecimento_relacao (fonte_id, origem_chave, destino_chave, tipo) VALUES (1, 'a', 'b', 'fk')`,
    );
    const r = await repo.verificar();
    expect(r.relacoesSemDestino).toEqual([
      { origem: 'a', destino: 'b', tipo: 'fk' },
    ]);
  });

  it('CLI: carregar sem banco com --dry-run apenas analisa', async () => {
    const f = arquivo('c.md', DDL('NOME'));
    const saidas: string[] = [];
    const quebrado: Provedor = {
      conectar: () =>
        Promise.reject(
          Object.assign(new Error('recusada'), { code: 'ECONNREFUSED' }),
        ),
    };
    const codigo = await executarConhecimento(['carregar', f, '--dry-run'], {
      provedor: quebrado,
      schema: 'adm',
      escrever: (t) => saidas.push(t),
    });
    expect(codigo).toBe(0);
    expect(saidas.join('\n')).toContain('Sem conexão com o banco');

    await expect(
      executarConhecimento(['carregar', f], {
        provedor: quebrado,
        schema: 'adm',
        escrever: () => undefined,
      }),
    ).rejects.toThrow('desligado por inatividade');
  });
});
