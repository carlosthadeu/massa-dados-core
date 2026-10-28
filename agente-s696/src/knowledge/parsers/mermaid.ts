export interface TransicaoDiagrama {
  origem: string;
  destino: string;
  acao: string;
}

export interface DiagramaEstados {
  estados: Map<string, string>;
  transicoes: TransicaoDiagrama[];
}

export const MARCA_INICIO_FIM = '[*]';

const transicao = /^(\[\*\]|[\w-]+)\s*-->\s*(\[\*\]|[\w-]+)(?:\s*:\s*(.+))?$/;
const definicao = /^([\w-]+)\s*:\s*(.+)$/;

export function parseMermaidEstados(
  bloco: string,
): DiagramaEstados | undefined {
  const linhas = bloco
    .split(/\r?\n/)
    .map((linha) => linha.trim())
    .filter((linha) => linha && !linha.startsWith('%%'));
  if (!linhas.some((linha) => /^stateDiagram(-v2)?\b/.test(linha))) {
    return undefined;
  }

  const estados = new Map<string, string>();
  const transicoes: TransicaoDiagrama[] = [];
  const registrar = (id: string): void => {
    if (id !== MARCA_INICIO_FIM && !estados.has(id)) {
      estados.set(id, id);
    }
  };

  for (const linha of linhas) {
    const aresta = transicao.exec(linha);
    if (aresta) {
      registrar(aresta[1]);
      registrar(aresta[2]);
      transicoes.push({
        origem: aresta[1],
        destino: aresta[2],
        acao: (aresta[3] ?? '').trim(),
      });
      continue;
    }
    const rotulo = definicao.exec(linha);
    if (rotulo && !/^(note|state|direction)\b/i.test(linha)) {
      estados.set(rotulo[1], rotulo[2].trim());
    }
  }

  return { estados, transicoes };
}
