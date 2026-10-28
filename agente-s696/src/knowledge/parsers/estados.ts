import { MARCA_INICIO_FIM, parseMermaidEstados } from './mermaid.js';
import { Bloco } from './markdown.js';
import { normalizar, slug } from '../texto.js';
import {
  DocumentoExtraido,
  ItemExtraido,
  Pendencia,
  RelacaoExtraida,
  SinonimoExtraido,
} from '../types.js';

interface OndeEstado {
  tabela: string;
  coluna: string;
  valor?: string;
}

interface EstadoTmp {
  id: string;
  rotulo: string;
  rotuloDaTabela: boolean;
  slugRotulo: string;
  codigo?: string;
  termos: string[];
  resumo?: string;
  onde: OndeEstado[];
  inicial?: string;
  final?: boolean;
  daLista: boolean;
  noDiagrama: boolean;
  naTabela: boolean;
}

interface EntidadeTmp {
  nome: string;
  estados: Map<string, EstadoTmp>;
  temDiagrama: boolean;
  temTabela: boolean;
  transicoes: { origem: string; destino: string; acao: string }[];
}

const padraoOnde =
  /^`?([A-Za-z_]\w*)\.([A-Za-z_]\w*)`?\s*(?:(?:==?|:)\s*`?(.+?)`?)?$/;

function nomeDoContexto(contexto: string): string {
  return contexto.replace(/[:\s]+$/, '').trim();
}

function celulas(linha: string): string[] {
  return linha
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((celula) => celula.replace(/<br\s*\/?>/gi, ';').trim());
}

function vazia(celula: string | undefined): boolean {
  return !celula || /^[-?–—\s]*$/.test(celula);
}

function lerOnde(
  celula: string,
  avisos: string[],
  estado: string,
): OndeEstado[] {
  const resultado: OndeEstado[] = [];
  for (const parte of celula.split(';')) {
    const texto = parte.trim();
    if (vazia(texto)) {
      continue;
    }
    const encontrado = padraoOnde.exec(texto);
    if (!encontrado) {
      avisos.push(
        `Estado "${estado}": não foi possível interpretar "${texto}" como TABELA.COLUNA = valor.`,
      );
      continue;
    }
    resultado.push({
      tabela: encontrado[1],
      coluna: encontrado[2],
      valor: encontrado[3]?.trim(),
    });
  }
  return resultado;
}

export function extrairMaquinasDeEstados(blocos: Bloco[]): DocumentoExtraido {
  const entidades = new Map<string, EntidadeTmp>();
  const avisos: string[] = [];
  const divergencias: string[] = [];
  let explicita: string | undefined;
  let ultima: string | undefined;

  const entidade = (nome: string): EntidadeTmp => {
    const chave = slug(nome);
    let encontrada = entidades.get(chave);
    if (!encontrada) {
      encontrada = {
        nome,
        estados: new Map(),
        temDiagrama: false,
        temTabela: false,
        transicoes: [],
      };
      entidades.set(chave, encontrada);
    }
    return encontrada;
  };

  const achar = (
    ent: EntidadeTmp,
    id: string,
    rotulo?: string,
  ): EstadoTmp | undefined => {
    const alvo = slug(id);
    const alvoRotulo = rotulo ? slug(rotulo) : undefined;
    for (const estado of ent.estados.values()) {
      if (
        slug(estado.id) === alvo ||
        estado.slugRotulo === alvo ||
        (alvoRotulo && estado.slugRotulo === alvoRotulo)
      ) {
        return estado;
      }
    }
    return undefined;
  };

  const criar = (ent: EntidadeTmp, id: string, rotulo: string): EstadoTmp => {
    const estado: EstadoTmp = {
      id,
      rotulo,
      rotuloDaTabela: false,
      slugRotulo: slug(rotulo),
      termos: [],
      onde: [],
      daLista: false,
      noDiagrama: false,
      naTabela: false,
    };
    ent.estados.set(id, estado);
    return estado;
  };

  for (const bloco of blocos) {
    if (bloco.tipo === 'diretiva') {
      explicita = bloco.entidade;
      ultima = bloco.entidade;
      continue;
    }
    if (bloco.tipo === 'titulo') {
      explicita = bloco.texto;
      ultima = bloco.texto;
      continue;
    }

    if (bloco.tipo === 'lista') {
      if (!/estado/i.test(bloco.contexto)) {
        continue;
      }
      const nome = explicita ?? nomeDoContexto(bloco.contexto);
      const ent = entidade(nome);
      ultima = nome;
      for (const item of bloco.itens) {
        const estado =
          achar(ent, item.rotulo) ?? criar(ent, slug(item.rotulo), item.rotulo);
        estado.daLista = true;
        if (item.codigo !== '?') {
          estado.codigo = item.codigo;
        }
      }
      continue;
    }

    if (bloco.tipo === 'codigo') {
      const diagrama = parseMermaidEstados(bloco.conteudo);
      if (!diagrama) {
        continue;
      }
      const nome =
        explicita ?? (nomeDoContexto(bloco.contexto) || 'Diagrama de estados');
      const ent = entidade(nome);
      ultima = nome;
      ent.temDiagrama = true;
      for (const [id, rotulo] of diagrama.estados) {
        const estado = achar(ent, id, rotulo) ?? criar(ent, id, rotulo);
        estado.noDiagrama = true;
        if (!estado.rotuloDaTabela) {
          estado.rotulo = rotulo;
          estado.slugRotulo = slug(rotulo);
        }
      }
      for (const transicao of diagrama.transicoes) {
        if (transicao.origem === MARCA_INICIO_FIM) {
          const destino = achar(ent, transicao.destino);
          if (destino) {
            destino.inicial = transicao.acao || 'início';
          }
        } else if (transicao.destino === MARCA_INICIO_FIM) {
          const origem = achar(ent, transicao.origem);
          if (origem) {
            origem.final = true;
          }
        } else {
          ent.transicoes.push(transicao);
        }
      }
      continue;
    }

    if (bloco.tipo === 'tabela') {
      const linhasDados = bloco.linhas.filter(
        (linha) => !/^\|?[\s:|-]+\|?$/.test(linha),
      );
      if (linhasDados.length < 2) {
        continue;
      }
      const cabecalho = celulas(linhasDados[0]).map(normalizar);
      const coluna = (...nomes: string[]): number =>
        cabecalho.findIndex((texto) => nomes.some((n) => texto.includes(n)));
      const colId = cabecalho.findIndex(
        (texto) => texto.includes('estado') && !texto.includes('rotulo'),
      );
      const colRotulo = coluna('rotulo');
      const colTermos = coluna('termo');
      const colResumo = coluna('resumo', 'descricao');
      const colOnde = coluna('onde', 'dados', 'identificacao');
      if (
        colId < 0 ||
        (colRotulo < 0 && colTermos < 0 && colResumo < 0 && colOnde < 0)
      ) {
        continue;
      }
      const nome =
        explicita ?? ultima ?? (nomeDoContexto(bloco.contexto) || 'Estados');
      const ent = entidade(nome);
      ultima = nome;
      ent.temTabela = true;

      for (const linha of linhasDados.slice(1)) {
        const valores = celulas(linha);
        const id = valores[colId]?.replace(/`/g, '').trim();
        if (vazia(id)) {
          continue;
        }
        const rotulo = colRotulo >= 0 ? valores[colRotulo] : undefined;
        const estado =
          achar(ent, id, vazia(rotulo) ? undefined : rotulo) ??
          criar(ent, id, vazia(rotulo) ? id : (rotulo as string));
        estado.naTabela = true;
        if (!vazia(rotulo)) {
          estado.rotulo = rotulo as string;
          estado.slugRotulo = slug(rotulo as string);
          estado.rotuloDaTabela = true;
        }
        if (colTermos >= 0 && !vazia(valores[colTermos])) {
          for (const termo of valores[colTermos].split(/[,;]/)) {
            const limpo = termo.replace(/^["'`]|["'`]$/g, '').trim();
            if (limpo && !estado.termos.includes(limpo)) {
              estado.termos.push(limpo);
            }
          }
        }
        if (colResumo >= 0 && !vazia(valores[colResumo])) {
          estado.resumo = valores[colResumo];
        }
        if (colOnde >= 0 && !vazia(valores[colOnde])) {
          estado.onde = lerOnde(valores[colOnde], avisos, estado.rotulo);
        }
      }
    }
  }

  const itens: ItemExtraido[] = [];
  const relacoes: RelacaoExtraida[] = [];
  const sinonimos: SinonimoExtraido[] = [];

  for (const ent of entidades.values()) {
    const prefixo = `estado:${slug(ent.nome)}`;

    for (const estado of ent.estados.values()) {
      if (
        estado.naTabela &&
        !estado.noDiagrama &&
        !estado.daLista &&
        ent.temDiagrama
      ) {
        divergencias.push(
          `${ent.nome}: o estado "${estado.rotulo}" consta na tabela, mas não existe no diagrama.`,
        );
      }
      if (estado.noDiagrama && !estado.naTabela && ent.temTabela) {
        divergencias.push(
          `${ent.nome}: o estado "${estado.rotulo}" consta no diagrama, mas não na tabela.`,
        );
      }

      const onde = estado.onde.map((item) => ({
        ...item,
        valor: item.valor ?? estado.codigo,
      }));
      if (estado.codigo !== undefined) {
        const conflito = onde.find(
          (item) => item.valor !== undefined && item.valor !== estado.codigo,
        );
        if (conflito) {
          divergencias.push(
            `${ent.nome}: o estado "${estado.rotulo}" tem código ${estado.codigo} na lista e ${conflito.tabela}.${conflito.coluna} = ${conflito.valor} na tabela.`,
          );
        }
      }

      const pendencias: Pendencia[] = [];
      if (estado.termos.length === 0) {
        pendencias.push({
          codigo: 'termos_comando',
          descricao: 'sem termos de comando',
          bloqueante: true,
        });
      }
      if (onde.length === 0) {
        pendencias.push({
          codigo: 'tabela_coluna',
          descricao: 'sem tabela e coluna que identifiquem o estado nos dados',
          bloqueante: true,
        });
      }
      if (onde.length === 0 || onde.some((item) => item.valor === undefined)) {
        pendencias.push({
          codigo: 'codigo',
          descricao: 'sem código (valor) do estado nos dados',
          bloqueante: true,
        });
      }
      if (!estado.resumo) {
        pendencias.push({
          codigo: 'resumo',
          descricao: 'sem resumo do estado',
          bloqueante: false,
        });
      }

      const partes = [`${estado.rotulo} (entidade: ${ent.nome}).`];
      if (estado.resumo) {
        partes.push(estado.resumo);
      }
      if (estado.termos.length > 0) {
        partes.push(`Termos de comando: ${estado.termos.join(', ')}.`);
      }
      const dados = onde.filter((item) => item.valor !== undefined);
      if (dados.length > 0) {
        partes.push(
          `Nos dados: ${dados
            .map((item) => `${item.tabela}.${item.coluna} = ${item.valor}`)
            .join('; ')}.`,
        );
      }

      const chave = `${prefixo}:${estado.id}`;
      itens.push({
        tipo: 'estado',
        chave,
        titulo: estado.rotulo,
        descricao: partes.join(' '),
        estruturado: {
          entidade: ent.nome,
          estadoId: estado.id,
          rotulo: estado.rotulo,
          codigo: estado.codigo,
          termos: estado.termos,
          resumo: estado.resumo,
          onde,
          inicial: estado.inicial,
          final: estado.final,
        },
        status: pendencias.some((p) => p.bloqueante) ? 'pendente' : 'ativo',
        pendencias,
      });

      const termos = new Set([estado.rotulo, ...estado.termos]);
      for (const termo of termos) {
        sinonimos.push({ termo, equivalente: chave });
      }
    }

    const vistas = new Set<string>();
    for (const transicao of ent.transicoes) {
      const origem = achar(ent, transicao.origem);
      const destino = achar(ent, transicao.destino);
      if (!origem || !destino) {
        continue;
      }
      const identificador = `${origem.id}|${destino.id}|${transicao.acao}`;
      if (vistas.has(identificador)) {
        continue;
      }
      vistas.add(identificador);
      relacoes.push({
        origem: `${prefixo}:${origem.id}`,
        destino: `${prefixo}:${destino.id}`,
        tipo: 'transicao',
        rotulo: transicao.acao,
        estruturado: { entidade: ent.nome },
      });
    }
  }

  return { itens, relacoes, sinonimos, avisos, divergencias };
}
