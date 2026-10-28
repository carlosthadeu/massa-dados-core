import { TabelaDdl } from './ddl.js';
import { DocumentoExtraido, ItemExtraido, RelacaoExtraida } from '../types.js';

export function chaveEsquema(tabela: string): string {
  return `esquema:${tabela.toUpperCase()}`;
}

function descreverColuna(
  coluna: TabelaDdl['colunas'][number],
  chavePrimaria: string[],
): string {
  const marcas = [
    coluna.tipo,
    coluna.anulavel ? 'NULL' : 'NOT NULL',
    coluna.identidade ? 'IDENTITY' : undefined,
    coluna.padrao ? `DEFAULT ${coluna.padrao}` : undefined,
    chavePrimaria.includes(coluna.nome) ? 'PK' : undefined,
  ].filter(Boolean);
  return `- ${coluna.nome} ${marcas.join(' ')}`;
}

export function tabelasParaDocumento(
  tabelas: { tabela: TabelaDdl; rotulo?: string }[],
): Pick<DocumentoExtraido, 'itens' | 'relacoes'> {
  const itens: ItemExtraido[] = [];
  const relacoes: RelacaoExtraida[] = [];

  for (const { tabela, rotulo } of tabelas) {
    const chave = chaveEsquema(tabela.nome);
    const referencias = tabela.chavesEstrangeiras.map(
      (fk) =>
        `${fk.tabelaReferenciada} (${fk.colunas.join(', ')} -> ${fk.colunasReferenciadas.join(', ')})`,
    );
    const linhas = [
      `Tabela ${tabela.nome}${rotulo ? ` (${rotulo})` : ''}.`,
      `Chave primária: ${tabela.chavePrimaria.join(', ') || 'não declarada'}.`,
      ...(referencias.length > 0
        ? [`Referencia: ${referencias.join('; ')}.`]
        : []),
      'Colunas:',
      ...tabela.colunas.map((coluna) =>
        descreverColuna(coluna, tabela.chavePrimaria),
      ),
    ];

    itens.push({
      tipo: 'esquema',
      chave,
      titulo: rotulo ? `${tabela.nome} - ${rotulo}` : tabela.nome,
      descricao: linhas.join('\n'),
      estruturado: {
        tabela: tabela.nome,
        rotulo,
        colunas: tabela.colunas,
        chavePrimaria: tabela.chavePrimaria,
        chavesEstrangeiras: tabela.chavesEstrangeiras,
        unicas: tabela.unicas,
      },
      status: 'ativo',
      pendencias: [],
    });

    const vistas = new Set<string>();
    for (const fk of tabela.chavesEstrangeiras) {
      const rotuloFk = `${fk.colunas.join(', ')} -> ${fk.colunasReferenciadas.join(', ')}`;
      const destino = chaveEsquema(fk.tabelaReferenciada);
      const identificador = `${destino}|${rotuloFk}`;
      if (vistas.has(identificador)) {
        continue;
      }
      vistas.add(identificador);
      relacoes.push({
        origem: chave,
        destino,
        tipo: 'fk',
        rotulo: rotuloFk,
        estruturado: { nome: fk.nome },
      });
    }
  }

  return { itens, relacoes };
}
