import type { Pool } from 'pg';
import { getPool } from '../persistence/db.js';
import { compararEstrutura, type MudancaEstrutura } from './diff.js';
import {
  citar,
  comandoTabelaMigracao,
  migracoes,
  TABELA_MIGRACAO,
} from './schema.js';
import { dividirEmTrechos, estimarTokens, hashConteudo } from './texto.js';
import type { DocumentoExtraido, ItemExtraido, Pendencia } from './types.js';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Linha = Record<string, any>;

export interface Executor {
  query(sql: string, params?: unknown[]): Promise<{ rows: Linha[] }>;
}

export interface Provedor {
  conectar<T>(fn: (executor: Executor) => Promise<T>): Promise<T>;
}

export function provedorPostgres(pool: Pool = getPool()): Provedor {
  return {
    async conectar(fn) {
      const client = await pool.connect();
      try {
        return await fn(client);
      } finally {
        client.release();
      }
    },
  };
}

export interface ResultadoMigracao {
  aplicadas: number[];
  pgvector: boolean;
  avisos: string[];
}

export interface PlanoDocumento {
  novos: string[];
  atualizados: string[];
  inalterados: string[];
  ausentes: string[];
  mudancasEstrutura: MudancaEstrutura[];
}

export interface PendenciaRegistrada {
  chave: string;
  tipo: string;
  titulo: string;
  status: string;
  pendencias: Pendencia[];
}

export interface CargaRegistrada {
  id: number;
  executor: string;
  iniciadaEm: string;
  concluidaEm: string | null;
  resumo: Record<string, unknown>;
}

export interface ResultadoVerificacao {
  relacoesSemDestino: { origem: string; destino: string; tipo: string }[];
  itensSemTrecho: string[];
  pgvector: boolean;
  totais: Record<string, number>;
}

interface Vigente {
  id: number;
  chave: string;
  versao: number;
  hash: string;
  estruturado: Record<string, unknown>;
  fonteId: number | null;
}

function hashItem(item: ItemExtraido): string {
  return hashConteudo({
    tipo: item.tipo,
    titulo: item.titulo,
    descricao: item.descricao,
    estruturado: item.estruturado,
    status: item.status,
    pendencias: item.pendencias,
  });
}

export class RepositorioConhecimento {
  private readonly s: string;

  constructor(
    private readonly provedor: Provedor,
    schema: string,
  ) {
    this.s = citar(schema);
  }

  private t(nome: string): string {
    return `${this.s}.${nome}`;
  }

  async migrar(): Promise<ResultadoMigracao> {
    return this.provedor.conectar(async (ex) => {
      const schema = this.s.slice(1, -1).replace(/""/g, '"');
      await ex.query(comandoTabelaMigracao(schema));
      const aplicadas = (
        await ex.query(`SELECT versao FROM ${this.t(TABELA_MIGRACAO)}`)
      ).rows.map((r) => Number(r.versao));

      const novas: number[] = [];
      for (const migracao of migracoes(schema)) {
        if (aplicadas.includes(migracao.versao)) continue;
        await ex.query('BEGIN');
        try {
          for (const comando of migracao.comandos) await ex.query(comando);
          await ex.query(
            `INSERT INTO ${this.t(TABELA_MIGRACAO)} (versao, descricao) VALUES ($1, $2)`,
            [migracao.versao, migracao.descricao],
          );
          await ex.query('COMMIT');
        } catch (erro) {
          await ex.query('ROLLBACK');
          throw erro;
        }
        novas.push(migracao.versao);
      }

      const avisos: string[] = [];
      let pgvector = await this.temPgvector(ex);
      if (!pgvector) {
        try {
          await ex.query('CREATE EXTENSION IF NOT EXISTS vector');
          pgvector = await this.temPgvector(ex);
        } catch (erro) {
          avisos.push(
            `pgvector indisponível (${(erro as Error).message}); necessário só a partir da Fase 4.`,
          );
        }
      }
      return { aplicadas: novas, pgvector, avisos };
    });
  }

  private async temPgvector(ex: Executor): Promise<boolean> {
    const r = await ex.query(
      `SELECT 1 FROM pg_extension WHERE extname = 'vector'`,
    );
    return r.rows.length > 0;
  }

  iniciarCarga(executor: string): Promise<number> {
    return this.provedor.conectar(async (ex) => {
      const r = await ex.query(
        `INSERT INTO ${this.t('conhecimento_carga')} (executor) VALUES ($1) RETURNING id`,
        [executor],
      );
      return Number(r.rows[0].id);
    });
  }

  concluirCarga(id: number, resumo: Record<string, unknown>): Promise<void> {
    return this.provedor.conectar(async (ex) => {
      await ex.query(
        `UPDATE ${this.t('conhecimento_carga')} SET concluida_em = now(), resumo = $2::jsonb WHERE id = $1`,
        [id, JSON.stringify(resumo)],
      );
    });
  }

  buscarHashFonte(caminho: string): Promise<string | undefined> {
    return this.provedor.conectar(async (ex) => {
      const r = await ex.query(
        `SELECT hash FROM ${this.t('conhecimento_fonte')} WHERE caminho = $1`,
        [caminho],
      );
      return r.rows[0]?.hash as string | undefined;
    });
  }

  private async vigentes(ex: Executor, chaves: string[]): Promise<Vigente[]> {
    if (chaves.length === 0) return [];
    const r = await ex.query(
      `SELECT id, chave_logica, versao, hash_conteudo, estruturado, fonte_id
         FROM ${this.t('conhecimento_item')}
        WHERE vigente AND chave_logica = ANY($1::text[])`,
      [chaves],
    );
    return r.rows.map((row) => ({
      id: Number(row.id),
      chave: row.chave_logica,
      versao: Number(row.versao),
      hash: row.hash_conteudo,
      estruturado: row.estruturado ?? {},
      fonteId: row.fonte_id === null ? null : Number(row.fonte_id),
    }));
  }

  private async ausentes(
    ex: Executor,
    caminho: string,
    chaves: string[],
  ): Promise<string[]> {
    const r = await ex.query(
      `SELECT i.chave_logica
         FROM ${this.t('conhecimento_item')} i
         JOIN ${this.t('conhecimento_fonte')} f ON f.id = i.fonte_id
        WHERE i.vigente AND f.caminho = $1 AND NOT (i.chave_logica = ANY($2::text[]))
        ORDER BY i.chave_logica`,
      [caminho, chaves],
    );
    return r.rows.map((row) => row.chave_logica as string);
  }

  private classificar(
    documento: DocumentoExtraido,
    existentes: Vigente[],
  ): PlanoDocumento {
    const porChave = new Map(existentes.map((v) => [v.chave, v]));
    const plano: PlanoDocumento = {
      novos: [],
      atualizados: [],
      inalterados: [],
      ausentes: [],
      mudancasEstrutura: [],
    };
    for (const item of documento.itens) {
      const atual = porChave.get(item.chave);
      if (!atual) plano.novos.push(item.chave);
      else if (atual.hash === hashItem(item))
        plano.inalterados.push(item.chave);
      else {
        plano.atualizados.push(item.chave);
        if (item.tipo === 'esquema') {
          const mudanca = compararEstrutura(
            item.chave,
            atual.estruturado,
            item.estruturado,
          );
          if (mudanca) plano.mudancasEstrutura.push(mudanca);
        }
      }
    }
    return plano;
  }

  /** Compara o documento com o banco sem gravar nada. */
  simular(
    caminho: string,
    documento: DocumentoExtraido,
  ): Promise<PlanoDocumento> {
    return this.provedor.conectar(async (ex) => {
      const chaves = documento.itens.map((i) => i.chave);
      const plano = this.classificar(
        documento,
        await this.vigentes(ex, chaves),
      );
      plano.ausentes = await this.ausentes(ex, caminho, chaves);
      return plano;
    });
  }

  /** Grava um documento em uma única transação. */
  gravarDocumento(
    caminho: string,
    hash: string,
    cargaId: number,
    documento: DocumentoExtraido,
  ): Promise<PlanoDocumento> {
    return this.provedor.conectar(async (ex) => {
      await ex.query('BEGIN');
      try {
        const fonte = await ex.query(
          `INSERT INTO ${this.t('conhecimento_fonte')} (caminho, hash, carga_id)
           VALUES ($1, $2, $3)
           ON CONFLICT (caminho) DO UPDATE
             SET hash = EXCLUDED.hash, carga_id = EXCLUDED.carga_id, atualizada_em = now()
           RETURNING id`,
          [caminho, hash, cargaId],
        );
        const fonteId = Number(fonte.rows[0].id);

        const chaves = documento.itens.map((i) => i.chave);
        const existentes = await this.vigentes(ex, chaves);
        const plano = this.classificar(documento, existentes);
        plano.ausentes = await this.ausentes(ex, caminho, chaves);
        const porChave = new Map(existentes.map((v) => [v.chave, v]));

        for (const item of documento.itens) {
          const atual = porChave.get(item.chave);
          if (atual && atual.hash === hashItem(item)) continue;
          if (atual) {
            await ex.query(
              `UPDATE ${this.t('conhecimento_item')} SET vigente = false WHERE id = $1`,
              [atual.id],
            );
            await ex.query(
              `DELETE FROM ${this.t('conhecimento_trecho')} WHERE item_id = $1`,
              [atual.id],
            );
          }
          const inserido = await ex.query(
            `INSERT INTO ${this.t('conhecimento_item')}
               (tipo, chave_logica, versao, titulo, descricao, estruturado, status,
                pendencias, hash_conteudo, fonte_id, carga_id)
             VALUES ($1, $2, $3, $4, $5, $6::jsonb, $7, $8::jsonb, $9, $10, $11)
             RETURNING id`,
            [
              item.tipo,
              item.chave,
              (atual?.versao ?? 0) + 1,
              item.titulo,
              item.descricao,
              JSON.stringify(item.estruturado),
              item.status,
              JSON.stringify(item.pendencias),
              hashItem(item),
              fonteId,
              cargaId,
            ],
          );
          const itemId = Number(inserido.rows[0].id);
          const trechos = dividirEmTrechos(
            [item.titulo, item.descricao].filter(Boolean).join('\n'),
          );
          for (const [ordem, texto] of trechos.entries()) {
            await ex.query(
              `INSERT INTO ${this.t('conhecimento_trecho')} (item_id, ordem, texto, tokens)
               VALUES ($1, $2, $3, $4)`,
              [itemId, ordem, texto, estimarTokens(texto)],
            );
          }
        }

        await ex.query(
          `DELETE FROM ${this.t('conhecimento_relacao')} WHERE fonte_id = $1`,
          [fonteId],
        );
        for (const r of documento.relacoes) {
          await ex.query(
            `INSERT INTO ${this.t('conhecimento_relacao')}
               (fonte_id, origem_chave, destino_chave, tipo, rotulo, estruturado)
             VALUES ($1, $2, $3, $4, $5, $6::jsonb)`,
            [
              fonteId,
              r.origem,
              r.destino,
              r.tipo,
              r.rotulo,
              JSON.stringify(r.estruturado),
            ],
          );
        }
        await ex.query(
          `DELETE FROM ${this.t('conhecimento_sinonimo')} WHERE fonte_id = $1`,
          [fonteId],
        );
        for (const sinonimo of documento.sinonimos) {
          await ex.query(
            `INSERT INTO ${this.t('conhecimento_sinonimo')} (fonte_id, termo, equivalente)
             VALUES ($1, $2, $3)`,
            [fonteId, sinonimo.termo, sinonimo.equivalente],
          );
        }
        await ex.query('COMMIT');
        return plano;
      } catch (erro) {
        await ex.query('ROLLBACK');
        throw erro;
      }
    });
  }

  listarPendencias(filtro?: string): Promise<PendenciaRegistrada[]> {
    return this.provedor.conectar(async (ex) => {
      const r = await ex.query(
        `SELECT chave_logica, tipo, titulo, status, pendencias
           FROM ${this.t('conhecimento_item')}
          WHERE vigente AND jsonb_array_length(pendencias) > 0
            AND ($1::text IS NULL OR chave_logica ILIKE '%' || $1 || '%')
          ORDER BY chave_logica`,
        [filtro ?? null],
      );
      return r.rows.map((row) => ({
        chave: row.chave_logica,
        tipo: row.tipo,
        titulo: row.titulo,
        status: row.status,
        pendencias: row.pendencias as Pendencia[],
      }));
    });
  }

  listarCargas(limite = 10): Promise<CargaRegistrada[]> {
    return this.provedor.conectar(async (ex) => {
      const r = await ex.query(
        `SELECT id, executor, iniciada_em, concluida_em, resumo
           FROM ${this.t('conhecimento_carga')} ORDER BY id DESC LIMIT $1`,
        [limite],
      );
      return r.rows.map((row) => ({
        id: Number(row.id),
        executor: row.executor,
        iniciadaEm: new Date(row.iniciada_em).toISOString(),
        concluidaEm: row.concluida_em
          ? new Date(row.concluida_em).toISOString()
          : null,
        resumo: row.resumo ?? {},
      }));
    });
  }

  verificar(): Promise<ResultadoVerificacao> {
    return this.provedor.conectar(async (ex) => {
      const semDestino = await ex.query(
        `SELECT r.origem_chave, r.destino_chave, r.tipo
           FROM ${this.t('conhecimento_relacao')} r
          WHERE NOT EXISTS (
            SELECT 1 FROM ${this.t('conhecimento_item')} i
             WHERE i.vigente AND i.chave_logica = r.destino_chave)
          ORDER BY r.origem_chave, r.destino_chave`,
      );
      const semTrecho = await ex.query(
        `SELECT i.chave_logica FROM ${this.t('conhecimento_item')} i
          WHERE i.vigente AND NOT EXISTS (
            SELECT 1 FROM ${this.t('conhecimento_trecho')} t WHERE t.item_id = i.id)
          ORDER BY i.chave_logica`,
      );
      const totais = await ex.query(
        `SELECT tipo, count(*)::int AS total FROM ${this.t('conhecimento_item')}
          WHERE vigente GROUP BY tipo`,
      );
      return {
        relacoesSemDestino: semDestino.rows.map((row) => ({
          origem: row.origem_chave,
          destino: row.destino_chave,
          tipo: row.tipo,
        })),
        itensSemTrecho: semTrecho.rows.map((row) => row.chave_logica),
        pgvector: await this.temPgvector(ex),
        totais: Object.fromEntries(
          totais.rows.map((row) => [row.tipo, Number(row.total)]),
        ),
      };
    });
  }
}
