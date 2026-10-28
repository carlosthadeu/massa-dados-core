import type { RelatorioCarga } from './carga.js';
import type {
  CargaRegistrada,
  PendenciaRegistrada,
  ResultadoMigracao,
  ResultadoVerificacao,
} from './repo.js';

export function formatarCarga(r: RelatorioCarga): string {
  const linhas: string[] = [];
  if (r.dryRun) linhas.push('[dry-run] nada foi gravado.');
  if (r.semBanco) {
    linhas.push('Sem conexão com o banco: apenas análise dos arquivos.');
  }
  if (r.cargaId !== undefined) linhas.push(`Carga #${r.cargaId}`);

  for (const a of r.arquivos) {
    linhas.push('', `${a.caminho}: ${a.situacao}`);
    if (a.erro) linhas.push(`  ERRO: ${a.erro}`);
    if (a.situacao === 'inalterado') continue;
    if (!a.erro) linhas.push(`  itens reconhecidos: ${a.itens}`);
    if (a.plano) {
      const p = a.plano;
      linhas.push(
        `  novos: ${p.novos.length}, atualizados: ${p.atualizados.length}, inalterados: ${p.inalterados.length}, ausentes: ${p.ausentes.length}`,
      );
      for (const chave of p.ausentes) {
        linhas.push(`  ausente no documento (mantido no banco): ${chave}`);
      }
      for (const m of p.mudancasEstrutura) {
        linhas.push(`  ESTRUTURA ALTERADA ${m.chave}:`);
        for (const c of m.colunasIncluidas) linhas.push(`    + coluna ${c}`);
        for (const c of m.colunasRemovidas) linhas.push(`    - coluna ${c}`);
        for (const c of m.colunasAlteradas) {
          linhas.push(`    ~ ${c.coluna}: ${c.de} -> ${c.para}`);
        }
        if (m.chavePrimariaAlterada) linhas.push('    ~ chave primária');
        if (m.chavesEstrangeirasAlteradas)
          linhas.push('    ~ chaves estrangeiras');
      }
    }
    for (const aviso of a.avisos) linhas.push(`  aviso: ${aviso}`);
    for (const d of a.divergencias) linhas.push(`  divergência: ${d}`);
    for (const item of a.pendencias) {
      linhas.push(`  pendente ${item.chave}:`);
      for (const p of item.pendencias) {
        linhas.push(
          `    - ${p.descricao}${p.bloqueante ? ' (bloqueante)' : ''}`,
        );
      }
    }
  }

  const rejeitados = r.arquivos.filter(
    (a) => a.situacao === 'rejeitado',
  ).length;
  linhas.push(
    '',
    `Total: ${r.arquivos.length} arquivo(s), ${rejeitados} rejeitado(s).`,
  );
  return linhas.join('\n');
}

export function formatarMigracao(r: ResultadoMigracao): string {
  return [
    r.aplicadas.length
      ? `Migrações aplicadas: ${r.aplicadas.join(', ')}`
      : 'Banco já está atualizado.',
    `pgvector: ${r.pgvector ? 'habilitado' : 'não habilitado'}`,
    ...r.avisos.map((a) => `aviso: ${a}`),
  ].join('\n');
}

export function formatarPendencias(lista: PendenciaRegistrada[]): string {
  if (lista.length === 0) return 'Nenhuma pendência.';
  return lista
    .flatMap((i) => [
      `${i.chave} [${i.status}] ${i.titulo}`,
      ...i.pendencias.map(
        (p) => `  - ${p.descricao}${p.bloqueante ? ' (bloqueante)' : ''}`,
      ),
    ])
    .join('\n');
}

export function formatarCargas(lista: CargaRegistrada[]): string {
  if (lista.length === 0) return 'Nenhuma carga registrada.';
  return lista
    .map(
      (c) =>
        `#${c.id} ${c.iniciadaEm} por ${c.executor} ${c.concluidaEm ? 'concluída' : 'incompleta'} ${JSON.stringify(c.resumo)}`,
    )
    .join('\n');
}

export function formatarVerificacao(r: ResultadoVerificacao): string {
  const linhas = [
    `Itens vigentes: ${
      Object.entries(r.totais)
        .map(([tipo, total]) => `${tipo}=${total}`)
        .join(', ') || 'nenhum'
    }`,
    `pgvector: ${r.pgvector ? 'habilitado' : 'não habilitado'}`,
    `Relações sem destino: ${r.relacoesSemDestino.length}`,
    ...r.relacoesSemDestino.map(
      (x) => `  ${x.tipo}: ${x.origem} -> ${x.destino}`,
    ),
    `Itens sem trecho: ${r.itensSemTrecho.length}`,
    ...r.itensSemTrecho.map((x) => `  ${x}`),
  ];
  return linhas.join('\n');
}
