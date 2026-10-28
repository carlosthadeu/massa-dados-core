export interface ColunaDdl {
  nome: string;
  tipo: string;
  anulavel: boolean;
  identidade?: boolean;
  padrao?: string;
}

export interface ChaveEstrangeiraDdl {
  nome?: string;
  colunas: string[];
  tabelaReferenciada: string;
  colunasReferenciadas: string[];
}

export interface TabelaDdl {
  nome: string;
  colunas: ColunaDdl[];
  chavePrimaria: string[];
  chavesEstrangeiras: ChaveEstrangeiraDdl[];
  unicas: string[][];
}

const referencia =
  /FOREIGN\s+KEY\s*\(([^)]*)\)\s*REFERENCES\s+([\w.]+)\s*\(([^)]*)\)/i;

function ultimoSegmento(nome: string): string {
  const partes = nome.split('.');
  return partes[partes.length - 1];
}

function lista(texto: string): string[] {
  return texto
    .split(',')
    .map((item) => item.trim().replace(/\s+(ASC|DESC)$/i, ''))
    .filter(Boolean);
}

function limparComentarios(sql: string): string {
  return sql
    .replace(/\/\*[\s\S]*?\*\//g, ' ')
    .replace(/--[^\n]*/g, ' ')
    .replace(/\[([^\]]+)\]/g, '$1')
    .replace(/"([^"]+)"/g, '$1');
}

function dividirNoNivelRaiz(corpo: string): string[] {
  const partes: string[] = [];
  let profundidade = 0;
  let atual = '';
  for (const caractere of corpo) {
    if (caractere === '(') {
      profundidade += 1;
    } else if (caractere === ')') {
      profundidade -= 1;
    }
    if (caractere === ',' && profundidade === 0) {
      partes.push(atual.trim());
      atual = '';
    } else {
      atual += caractere;
    }
  }
  if (atual.trim()) {
    partes.push(atual.trim());
  }
  return partes;
}

function lerColuna(definicao: string, tabela: TabelaDdl): void {
  const encontrado = /^(\w+)\s+(\w+)(\s*\(\s*[^)]*\))?(.*)$/s.exec(definicao);
  if (!encontrado) {
    return;
  }
  const [, nome, tipoBase, parametros, resto] = encontrado;
  const tipo = `${tipoBase}${(parametros ?? '').replace(/\s+/g, '')}`;
  const padrao = /\bDEFAULT\s+(\([^)]*\)|'[^']*'|[^\s,]+)/i.exec(resto)?.[1];
  const coluna: ColunaDdl = {
    nome,
    tipo,
    anulavel: !/\bNOT\s+NULL\b/i.test(resto),
  };
  if (/\bIDENTITY\b/i.test(resto)) {
    coluna.identidade = true;
  }
  if (padrao) {
    coluna.padrao = padrao;
  }
  tabela.colunas.push(coluna);
  if (/\bPRIMARY\s+KEY\b/i.test(resto)) {
    tabela.chavePrimaria = [nome];
  }
  const inline = /\bREFERENCES\s+([\w.]+)\s*\(([^)]*)\)/i.exec(resto);
  if (inline) {
    tabela.chavesEstrangeiras.push({
      colunas: [nome],
      tabelaReferenciada: ultimoSegmento(inline[1]),
      colunasReferenciadas: lista(inline[2]),
    });
  }
}

function lerParte(parte: string, tabela: TabelaDdl): void {
  const comNome = /^CONSTRAINT\s+(\S+)\s+(.*)$/is.exec(parte);
  const nome = comNome?.[1];
  const texto = comNome ? comNome[2] : parte;

  const chavePrimaria =
    /^PRIMARY\s+KEY\s*(?:NON)?(?:CLUSTERED\s*)?\(([^)]*)\)/i.exec(texto);
  if (chavePrimaria) {
    tabela.chavePrimaria = lista(chavePrimaria[1]);
    return;
  }
  const estrangeira = referencia.exec(texto);
  if (/^FOREIGN\s+KEY/i.test(texto) && estrangeira) {
    tabela.chavesEstrangeiras.push({
      nome,
      colunas: lista(estrangeira[1]),
      tabelaReferenciada: ultimoSegmento(estrangeira[2]),
      colunasReferenciadas: lista(estrangeira[3]),
    });
    return;
  }
  const unica = /^UNIQUE\s*(?:NON)?(?:CLUSTERED\s*)?\(([^)]*)\)/i.exec(texto);
  if (unica) {
    tabela.unicas.push(lista(unica[1]));
    return;
  }
  if (comNome || /^(CHECK|INDEX|KEY)\b/i.test(texto)) {
    return;
  }
  lerColuna(texto, tabela);
}

export function parseDdl(sql: string): {
  tabelas: TabelaDdl[];
  avisos: string[];
} {
  const texto = limparComentarios(sql);
  const tabelas: TabelaDdl[] = [];
  const avisos: string[] = [];

  const criar = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([\w.]+)\s*\(/gi;
  let correspondencia: RegExpExecArray | null;
  while ((correspondencia = criar.exec(texto)) !== null) {
    const inicio = correspondencia.index + correspondencia[0].length;
    let profundidade = 1;
    let fim = inicio;
    while (fim < texto.length && profundidade > 0) {
      if (texto[fim] === '(') {
        profundidade += 1;
      } else if (texto[fim] === ')') {
        profundidade -= 1;
      }
      fim += 1;
    }
    if (profundidade !== 0) {
      avisos.push(
        `CREATE TABLE ${correspondencia[1]}: parênteses não fecham; tabela ignorada.`,
      );
      continue;
    }
    const tabela: TabelaDdl = {
      nome: ultimoSegmento(correspondencia[1]),
      colunas: [],
      chavePrimaria: [],
      chavesEstrangeiras: [],
      unicas: [],
    };
    for (const parte of dividirNoNivelRaiz(texto.slice(inicio, fim - 1))) {
      lerParte(parte, tabela);
    }
    tabelas.push(tabela);
  }

  const alterar =
    /ALTER\s+TABLE\s+([\w.]+)\s+(?:WITH\s+\w+\s+)?ADD\s+(?:CONSTRAINT\s+(\w+)\s+)?((?:FOREIGN|PRIMARY)\s+KEY[^;]*)/gi;
  while ((correspondencia = alterar.exec(texto)) !== null) {
    const alvo = tabelas.find(
      (tabela) =>
        tabela.nome.toUpperCase() ===
        ultimoSegmento(correspondencia![1]).toUpperCase(),
    );
    if (!alvo) {
      avisos.push(
        `ALTER TABLE ${correspondencia[1]}: a tabela não tem CREATE TABLE neste documento.`,
      );
      continue;
    }
    const definicao = correspondencia[2]
      ? `CONSTRAINT ${correspondencia[2]} ${correspondencia[3]}`
      : correspondencia[3];
    lerParte(definicao.trim(), alvo);
  }

  for (const tabela of tabelas) {
    for (const coluna of tabela.colunas) {
      if (tabela.chavePrimaria.includes(coluna.nome)) {
        coluna.anulavel = false;
      }
    }
  }

  return { tabelas, avisos };
}
