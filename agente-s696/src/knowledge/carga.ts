import { readdir, readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { extrairDocumento, EXTENSOES_SUPORTADAS } from './parsers/documento.js';
import type { PlanoDocumento, RepositorioConhecimento } from './repo.js';
import { hashBuffer, lerTextoUtf8 } from './texto.js';
import type { Pendencia } from './types.js';

export interface OpcoesCarga {
  dryRun?: boolean;
  forcar?: boolean;
  executor: string;
}

export type SituacaoArquivo =
  'carregado' | 'inalterado' | 'simulado' | 'analisado' | 'rejeitado';

export interface ResultadoArquivo {
  caminho: string;
  situacao: SituacaoArquivo;
  plano?: PlanoDocumento;
  itens: number;
  avisos: string[];
  divergencias: string[];
  pendencias: { chave: string; pendencias: Pendencia[] }[];
  erro?: string;
}

export interface RelatorioCarga {
  cargaId?: number;
  dryRun: boolean;
  semBanco: boolean;
  arquivos: ResultadoArquivo[];
}

const IGNORADOS = new Set(['node_modules', '.git', 'dist']);

export async function listarArquivos(entradas: string[]): Promise<string[]> {
  const encontrados: string[] = [];
  async function visitar(caminho: string): Promise<void> {
    const info = await stat(caminho);
    if (info.isDirectory()) {
      for (const nome of (await readdir(caminho)).sort()) {
        if (!IGNORADOS.has(nome)) await visitar(path.join(caminho, nome));
      }
    } else if (
      EXTENSOES_SUPORTADAS.includes(path.extname(caminho).toLowerCase())
    ) {
      encontrados.push(caminho);
    }
  }
  for (const entrada of entradas) await visitar(path.resolve(entrada));
  return encontrados;
}

function caminhoLogico(caminho: string): string {
  return path.relative(process.cwd(), caminho).split(path.sep).join('/');
}

export async function carregar(
  repo: RepositorioConhecimento | undefined,
  entradas: string[],
  opcoes: OpcoesCarga,
): Promise<RelatorioCarga> {
  const arquivos = await listarArquivos(entradas);
  const dryRun = opcoes.dryRun ?? false;
  const relatorio: RelatorioCarga = {
    dryRun,
    semBanco: repo === undefined,
    arquivos: [],
  };
  const cargaId =
    repo && !dryRun ? await repo.iniciarCarga(opcoes.executor) : undefined;
  relatorio.cargaId = cargaId;

  for (const arquivo of arquivos) {
    const caminho = caminhoLogico(arquivo);
    const resultado: ResultadoArquivo = {
      caminho,
      situacao: 'analisado',
      itens: 0,
      avisos: [],
      divergencias: [],
      pendencias: [],
    };
    relatorio.arquivos.push(resultado);
    try {
      const dados = await readFile(arquivo);
      const hash = hashBuffer(dados);
      if (repo && !opcoes.forcar && !dryRun) {
        if ((await repo.buscarHashFonte(caminho)) === hash) {
          resultado.situacao = 'inalterado';
          continue;
        }
      }
      const documento = extrairDocumento(caminho, lerTextoUtf8(dados));
      resultado.itens = documento.itens.length;
      resultado.avisos = documento.avisos;
      resultado.divergencias = documento.divergencias;
      resultado.pendencias = documento.itens
        .filter((i) => i.pendencias.length > 0)
        .map((i) => ({ chave: i.chave, pendencias: i.pendencias }));

      if (!repo) continue;
      if (dryRun) {
        resultado.plano = await repo.simular(caminho, documento);
        resultado.situacao = 'simulado';
      } else {
        resultado.plano = await repo.gravarDocumento(
          caminho,
          hash,
          cargaId as number,
          documento,
        );
        resultado.situacao = 'carregado';
      }
    } catch (erro) {
      resultado.situacao = 'rejeitado';
      resultado.erro = (erro as Error).message;
    }
  }

  if (repo && cargaId !== undefined) {
    await repo.concluirCarga(cargaId, {
      arquivos: relatorio.arquivos.length,
      carregados: relatorio.arquivos.filter((a) => a.situacao === 'carregado')
        .length,
      rejeitados: relatorio.arquivos.filter((a) => a.situacao === 'rejeitado')
        .length,
    });
  }
  return relatorio;
}
