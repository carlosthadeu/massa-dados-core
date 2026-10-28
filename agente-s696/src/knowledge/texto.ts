import { createHash } from 'node:crypto';

export function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

export function slug(texto: string): string {
  return normalizar(texto)
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function serializarEstavel(valor: unknown): string {
  if (valor === undefined || valor === null) {
    return 'null';
  }
  if (Array.isArray(valor)) {
    return `[${valor.map(serializarEstavel).join(',')}]`;
  }
  if (typeof valor === 'object') {
    const entradas = Object.entries(valor as Record<string, unknown>)
      .filter(([, item]) => item !== undefined)
      .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
      .map(
        ([chave, item]) =>
          `${JSON.stringify(chave)}:${serializarEstavel(item)}`,
      );
    return `{${entradas.join(',')}}`;
  }
  return JSON.stringify(valor);
}

export function hashConteudo(valor: unknown): string {
  return createHash('sha256').update(serializarEstavel(valor)).digest('hex');
}

export function hashBuffer(dados: Buffer): string {
  return createHash('sha256').update(dados).digest('hex');
}

export function lerTextoUtf8(dados: Buffer): string {
  const semBom =
    dados.length >= 3 &&
    dados[0] === 0xef &&
    dados[1] === 0xbb &&
    dados[2] === 0xbf
      ? dados.subarray(3)
      : dados;
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(semBom);
  } catch {
    throw new Error('O arquivo não está em UTF-8 válido.');
  }
}

export function estimarTokens(texto: string): number {
  return Math.max(1, Math.ceil(texto.length / 4));
}

function quebrar(linha: string, limite: number): string[] {
  const pedacos: string[] = [];
  let atual = '';
  for (const frase of linha.split(/(?<=[.;!?])\s+/)) {
    if (frase.length > limite) {
      if (atual) {
        pedacos.push(atual);
        atual = '';
      }
      for (let inicio = 0; inicio < frase.length; inicio += limite) {
        pedacos.push(frase.slice(inicio, inicio + limite));
      }
    } else if (atual && atual.length + 1 + frase.length > limite) {
      pedacos.push(atual);
      atual = frase;
    } else {
      atual = atual ? `${atual} ${frase}` : frase;
    }
  }
  if (atual) {
    pedacos.push(atual);
  }
  return pedacos;
}

export function dividirEmTrechos(texto: string, limite = 700): string[] {
  const partes: string[] = [];
  for (const linha of texto.split(/\r?\n/)) {
    const aparada = linha.trim();
    if (!aparada) {
      continue;
    }
    if (aparada.length <= limite) {
      partes.push(aparada);
    } else {
      partes.push(...quebrar(aparada, limite));
    }
  }
  const trechos: string[] = [];
  let atual = '';
  for (const parte of partes) {
    if (atual && atual.length + 1 + parte.length > limite) {
      trechos.push(atual);
      atual = parte;
    } else {
      atual = atual ? `${atual}\n${parte}` : parte;
    }
  }
  if (atual) {
    trechos.push(atual);
  }
  return trechos;
}
