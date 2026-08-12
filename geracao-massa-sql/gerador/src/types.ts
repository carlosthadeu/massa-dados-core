import { AcaoValoresFixos } from './templates/acao.js';

/**
 * Interface para os valores fixos opcionais das ações, etapas e entregas.
 */
export interface ValoresFixos extends AcaoValoresFixos {
  stEtpAco?: number;
  pcReaTot?: number;
  stRep?: number;
  stEntAco?: number;
  [key: string]: any;
}

/**
 * Parâmetros de entrada para a geração do script SQL completo.
 */
export interface GeradorParams {
  destino: number;
  quantidadeAcoes?: number;
  minEtapas?: number;
  maxEtapas?: number;
  valoresFixos?: ValoresFixos;
}

/**
 * Opções genéricas para geradores de texto/código.
 */
export interface GeneratorOptions {
  prefix?: string;
  suffix?: string;
  length?: number;
}

/**
 * Contrato base para geradores.
 */
export interface IGenerator<T> {
  generate(options?: GeneratorOptions): T;
}