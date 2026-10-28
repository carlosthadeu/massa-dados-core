export type TipoItem =
  'receita' | 'regra' | 'cenario' | 'esquema' | 'glossario' | 'estado';

export type StatusItem = 'ativo' | 'pendente' | 'candidata';

export interface Pendencia {
  codigo: string;
  descricao: string;
  bloqueante: boolean;
}

export interface ItemExtraido {
  tipo: TipoItem;
  chave: string;
  titulo: string;
  descricao: string;
  estruturado: Record<string, unknown>;
  status: StatusItem;
  pendencias: Pendencia[];
}

export interface RelacaoExtraida {
  origem: string;
  destino: string;
  tipo: string;
  rotulo: string;
  estruturado: Record<string, unknown>;
}

export interface SinonimoExtraido {
  termo: string;
  equivalente: string;
}

export interface DocumentoExtraido {
  itens: ItemExtraido[];
  relacoes: RelacaoExtraida[];
  sinonimos: SinonimoExtraido[];
  avisos: string[];
  divergencias: string[];
}
