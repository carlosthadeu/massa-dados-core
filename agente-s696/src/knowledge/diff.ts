export interface MudancaEstrutura {
  chave: string;
  colunasIncluidas: string[];
  colunasRemovidas: string[];
  colunasAlteradas: { coluna: string; de: string; para: string }[];
  chavePrimariaAlterada: boolean;
  chavesEstrangeirasAlteradas: boolean;
}

interface ColunaGuardada {
  nome: string;
  tipo: string;
  anulavel: boolean;
  padrao?: string;
  identidade?: boolean;
}

function colunasDe(estruturado: Record<string, unknown>): ColunaGuardada[] {
  const colunas = estruturado.colunas;
  return Array.isArray(colunas) ? (colunas as ColunaGuardada[]) : [];
}

function descrever(coluna: ColunaGuardada): string {
  return [
    coluna.tipo,
    coluna.anulavel ? 'NULL' : 'NOT NULL',
    coluna.padrao ? `DEFAULT ${coluna.padrao}` : '',
    coluna.identidade ? 'IDENTITY' : '',
  ]
    .filter(Boolean)
    .join(' ');
}

export function compararEstrutura(
  chave: string,
  anterior: Record<string, unknown>,
  atual: Record<string, unknown>,
): MudancaEstrutura | undefined {
  const antigas = new Map(colunasDe(anterior).map((c) => [c.nome, c]));
  const novas = new Map(colunasDe(atual).map((c) => [c.nome, c]));

  const mudanca: MudancaEstrutura = {
    chave,
    colunasIncluidas: [...novas.keys()].filter((nome) => !antigas.has(nome)),
    colunasRemovidas: [...antigas.keys()].filter((nome) => !novas.has(nome)),
    colunasAlteradas: [],
    chavePrimariaAlterada:
      JSON.stringify(anterior.chavePrimaria ?? []) !==
      JSON.stringify(atual.chavePrimaria ?? []),
    chavesEstrangeirasAlteradas:
      JSON.stringify(anterior.chavesEstrangeiras ?? []) !==
      JSON.stringify(atual.chavesEstrangeiras ?? []),
  };
  for (const [nome, nova] of novas) {
    const antiga = antigas.get(nome);
    if (antiga && descrever(antiga) !== descrever(nova)) {
      mudanca.colunasAlteradas.push({
        coluna: nome,
        de: descrever(antiga),
        para: descrever(nova),
      });
    }
  }

  const houveMudanca =
    mudanca.colunasIncluidas.length > 0 ||
    mudanca.colunasRemovidas.length > 0 ||
    mudanca.colunasAlteradas.length > 0 ||
    mudanca.chavePrimariaAlterada ||
    mudanca.chavesEstrangeirasAlteradas;
  return houveMudanca ? mudanca : undefined;
}
