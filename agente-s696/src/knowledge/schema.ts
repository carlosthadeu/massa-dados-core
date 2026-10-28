export interface Migracao {
  versao: number;
  descricao: string;
  comandos: string[];
}

export function citar(identificador: string): string {
  return `"${identificador.replace(/"/g, '""')}"`;
}

export function migracoes(schema: string): Migracao[] {
  const s = citar(schema);
  return [
    {
      versao: 1,
      descricao: 'Tabelas base do conhecimento de domínio',
      comandos: [
        `CREATE TABLE IF NOT EXISTS ${s}.conhecimento_carga (
          id BIGSERIAL PRIMARY KEY,
          executor TEXT NOT NULL,
          iniciada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
          concluida_em TIMESTAMPTZ,
          resumo JSONB NOT NULL DEFAULT '{}'::jsonb
        )`,
        `CREATE TABLE IF NOT EXISTS ${s}.conhecimento_fonte (
          id BIGSERIAL PRIMARY KEY,
          caminho TEXT NOT NULL UNIQUE,
          hash TEXT NOT NULL,
          carga_id BIGINT REFERENCES ${s}.conhecimento_carga(id),
          atualizada_em TIMESTAMPTZ NOT NULL DEFAULT now()
        )`,
        `CREATE TABLE IF NOT EXISTS ${s}.conhecimento_item (
          id BIGSERIAL PRIMARY KEY,
          tipo TEXT NOT NULL CHECK (tipo IN ('receita','regra','cenario','esquema','glossario','estado')),
          chave_logica TEXT NOT NULL,
          versao INTEGER NOT NULL,
          vigente BOOLEAN NOT NULL DEFAULT true,
          titulo TEXT NOT NULL,
          descricao TEXT NOT NULL DEFAULT '',
          estruturado JSONB NOT NULL DEFAULT '{}'::jsonb,
          status TEXT NOT NULL CHECK (status IN ('ativo','pendente','candidata')),
          pendencias JSONB NOT NULL DEFAULT '[]'::jsonb,
          hash_conteudo TEXT NOT NULL,
          fonte_id BIGINT REFERENCES ${s}.conhecimento_fonte(id),
          carga_id BIGINT REFERENCES ${s}.conhecimento_carga(id),
          criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
          UNIQUE (chave_logica, versao)
        )`,
        `CREATE UNIQUE INDEX IF NOT EXISTS conhecimento_item_vigente_uq
          ON ${s}.conhecimento_item (chave_logica) WHERE vigente`,
        `CREATE INDEX IF NOT EXISTS conhecimento_item_fonte_ix
          ON ${s}.conhecimento_item (fonte_id)`,
        `CREATE TABLE IF NOT EXISTS ${s}.conhecimento_trecho (
          id BIGSERIAL PRIMARY KEY,
          item_id BIGINT NOT NULL REFERENCES ${s}.conhecimento_item(id) ON DELETE CASCADE,
          ordem INTEGER NOT NULL,
          texto TEXT NOT NULL,
          tokens INTEGER NOT NULL,
          tsv TSVECTOR GENERATED ALWAYS AS (to_tsvector('portuguese', texto)) STORED
        )`,
        `CREATE INDEX IF NOT EXISTS conhecimento_trecho_tsv_ix
          ON ${s}.conhecimento_trecho USING GIN (tsv)`,
        `CREATE TABLE IF NOT EXISTS ${s}.conhecimento_relacao (
          id BIGSERIAL PRIMARY KEY,
          fonte_id BIGINT NOT NULL REFERENCES ${s}.conhecimento_fonte(id) ON DELETE CASCADE,
          origem_chave TEXT NOT NULL,
          destino_chave TEXT NOT NULL,
          tipo TEXT NOT NULL,
          rotulo TEXT NOT NULL DEFAULT '',
          estruturado JSONB NOT NULL DEFAULT '{}'::jsonb
        )`,
        `CREATE INDEX IF NOT EXISTS conhecimento_relacao_origem_ix
          ON ${s}.conhecimento_relacao (origem_chave)`,
        `CREATE INDEX IF NOT EXISTS conhecimento_relacao_destino_ix
          ON ${s}.conhecimento_relacao (destino_chave)`,
        `CREATE TABLE IF NOT EXISTS ${s}.conhecimento_sinonimo (
          id BIGSERIAL PRIMARY KEY,
          fonte_id BIGINT NOT NULL REFERENCES ${s}.conhecimento_fonte(id) ON DELETE CASCADE,
          termo TEXT NOT NULL,
          equivalente TEXT NOT NULL
        )`,
        `CREATE INDEX IF NOT EXISTS conhecimento_sinonimo_termo_ix
          ON ${s}.conhecimento_sinonimo (termo)`,
      ],
    },
  ];
}

export const TABELA_MIGRACAO = 'conhecimento_migracao';

export function comandoTabelaMigracao(schema: string): string {
  return `CREATE TABLE IF NOT EXISTS ${citar(schema)}.${TABELA_MIGRACAO} (
    versao INTEGER PRIMARY KEY,
    descricao TEXT NOT NULL,
    aplicada_em TIMESTAMPTZ NOT NULL DEFAULT now()
  )`;
}
