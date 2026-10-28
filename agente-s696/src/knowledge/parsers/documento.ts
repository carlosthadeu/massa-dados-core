import path from 'node:path';
import { parseDdl, TabelaDdl } from './ddl.js';
import { tabelasParaDocumento } from './esquema.js';
import { extrairMaquinasDeEstados } from './estados.js';
import { segmentar } from './markdown.js';
import { DocumentoExtraido } from '../types.js';

const linguagensCsv = new Set(['plaintext', 'planitext', 'csv', 'text', 'txt']);

export const EXTENSOES_SUPORTADAS = ['.md', '.txt', '.sql'];

export function extrairDocumento(
  caminho: string,
  texto: string,
): DocumentoExtraido {
  const extensao = path.extname(caminho).toLowerCase();
  const avisos: string[] = [];
  const tabelas: { tabela: TabelaDdl; rotulo?: string }[] = [];
  const registrar = (lidas: TabelaDdl[], rotulo?: string): void => {
    for (const tabela of lidas) {
      const indice = tabelas.findIndex(
        (existente) =>
          existente.tabela.nome.toUpperCase() === tabela.nome.toUpperCase(),
      );
      if (indice >= 0) {
        avisos.push(
          `A tabela ${tabela.nome} aparece mais de uma vez no documento; vale a última definição.`,
        );
        tabelas.splice(indice, 1);
      }
      tabelas.push({ tabela, rotulo });
    }
  };

  if (extensao === '.sql') {
    const resultado = parseDdl(texto);
    avisos.push(...resultado.avisos);
    registrar(resultado.tabelas);
    return {
      ...tabelasParaDocumento(tabelas),
      sinonimos: [],
      avisos,
      divergencias: [],
    };
  }

  const blocos = segmentar(texto);
  const estados = extrairMaquinasDeEstados(blocos);
  avisos.push(...estados.avisos);

  let rotuloAtual: string | undefined;
  let blocosCsv = 0;
  for (const bloco of blocos) {
    if (bloco.tipo === 'titulo' && bloco.nivel === 2) {
      rotuloAtual = bloco.texto;
    } else if (bloco.tipo === 'codigo') {
      if (bloco.linguagem === 'sql') {
        const resultado = parseDdl(bloco.conteudo);
        avisos.push(...resultado.avisos);
        registrar(resultado.tabelas, rotuloAtual);
      } else if (linguagensCsv.has(bloco.linguagem)) {
        blocosCsv += 1;
      }
    }
  }

  const esquema = tabelasParaDocumento(tabelas);
  if (tabelas.length > 0 && blocosCsv > 0) {
    avisos.push(
      `${blocosCsv} bloco(s) CSV de exemplo reconhecido(s); eles serão tratados na Fase 3 e não foram carregados.`,
    );
  }

  const documento: DocumentoExtraido = {
    itens: [...estados.itens, ...esquema.itens],
    relacoes: [...estados.relacoes, ...esquema.relacoes],
    sinonimos: estados.sinonimos,
    avisos,
    divergencias: estados.divergencias,
  };
  if (documento.itens.length === 0) {
    documento.avisos.push(
      'Nenhum conteúdo reconhecido (máquina de estados ou DDL).',
    );
  }
  return documento;
}
