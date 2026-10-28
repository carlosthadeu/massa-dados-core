export type Bloco =
  | { tipo: 'titulo'; nivel: number; texto: string; linha: number }
  | { tipo: 'diretiva'; entidade: string; linha: number }
  | {
      tipo: 'codigo';
      linguagem: string;
      conteudo: string;
      contexto: string;
      linha: number;
    }
  | { tipo: 'tabela'; linhas: string[]; contexto: string; linha: number }
  | {
      tipo: 'lista';
      itens: { codigo: string; rotulo: string }[];
      contexto: string;
      linha: number;
    };

const abertura = /^\s*(`{3,}|~{3,})\s*([\w+-]*)/;
const titulo = /^(#{1,6})\s+(.+?)\s*#*\s*$/;
const diretiva = /^\s*(?:entidade|entity)\s*:\s*(.+?)\s*$/i;
const itemLista = /^\s*(\d+|\?)\s*[-–—:.)]\s+(.+?)\s*$/;

export function segmentar(texto: string): Bloco[] {
  const linhas = texto.split(/\r?\n/);
  const blocos: Bloco[] = [];
  let contexto = '';
  let indice = 0;

  while (indice < linhas.length) {
    const linha = linhas[indice];
    const numero = indice + 1;

    const cerca = abertura.exec(linha);
    if (cerca) {
      const marca = cerca[1];
      const fechamento = new RegExp(`^${marca[0]}{${marca.length},}$`);
      const corpo: string[] = [];
      indice += 1;
      while (
        indice < linhas.length &&
        !fechamento.test(linhas[indice].trim())
      ) {
        corpo.push(linhas[indice]);
        indice += 1;
      }
      indice += 1;
      blocos.push({
        tipo: 'codigo',
        linguagem: (cerca[2] ?? '').toLowerCase(),
        conteudo: corpo.join('\n'),
        contexto,
        linha: numero,
      });
      continue;
    }

    const cabecalho = titulo.exec(linha);
    if (cabecalho) {
      blocos.push({
        tipo: 'titulo',
        nivel: cabecalho[1].length,
        texto: cabecalho[2],
        linha: numero,
      });
      indice += 1;
      continue;
    }

    const entidade = diretiva.exec(linha);
    if (entidade) {
      blocos.push({ tipo: 'diretiva', entidade: entidade[1], linha: numero });
      indice += 1;
      continue;
    }

    if (linha.trim().startsWith('|')) {
      const tabela: string[] = [];
      while (indice < linhas.length && linhas[indice].trim().startsWith('|')) {
        tabela.push(linhas[indice].trim());
        indice += 1;
      }
      blocos.push({ tipo: 'tabela', linhas: tabela, contexto, linha: numero });
      continue;
    }

    if (itemLista.test(linha)) {
      const itens: { codigo: string; rotulo: string }[] = [];
      while (indice < linhas.length) {
        const encontrado = itemLista.exec(linhas[indice]);
        if (!encontrado) {
          break;
        }
        itens.push({ codigo: encontrado[1], rotulo: encontrado[2] });
        indice += 1;
      }
      blocos.push({ tipo: 'lista', itens, contexto, linha: numero });
      continue;
    }

    if (linha.trim()) {
      contexto = linha.trim();
    }
    indice += 1;
  }

  return blocos;
}
