import { gerarLoremIpsum } from '../utils/sql.js';
import { gerarTextoHistorico } from './historico.js';

export interface AcaoValoresFixos {
  sqPtfAco?: number;
  sqOriAco?: number;
  cdUnd?: number;
  cdUndApr?: number;
  stAcoEtt?: number;
  tpAcoEtt?: number;
  mtUsuAlt?: string;
  nmUsuAlt?: string;
  mtUsuRsp?: string;
  dhLim?: string;
  deHst?: string;
  [key: string]: any;
}

export function gerarInsertAcao(
  destino: number,
  numeroAcao: number,
  cdUndCalculado: number,
  valoresFixos?: AcaoValoresFixos
): string {
  const sqPtfAco = valoresFixos?.sqPtfAco ?? destino;
  const sqOriAco = valoresFixos?.sqOriAco ?? 1;
  const cdUnd = valoresFixos?.cdUnd ?? cdUndCalculado;
  const cdUndApr = valoresFixos?.cdUndApr ?? 694;
  
  const stAcoEtt = valoresFixos?.stAcoEtt ?? 10;
  const tpAcoEtt = valoresFixos?.tpAcoEtt ?? 3;
  
  const mtUsuAlt = valoresFixos?.mtUsuAlt ?? 'F856806';
  const nmUsuAlt = valoresFixos?.nmUsuAlt ?? (mtUsuAlt === 'F856806' ? 'Equipe do Ambiente de Planejamento' : 'Facilitador 1681');
  const mtUsuRsp = valoresFixos?.mtUsuRsp ?? 'F054135';

  const anoSubsequente = new Date().getFullYear() + 1;
  const dhLim = valoresFixos?.dhLim ?? `'${anoSubsequente}-12-31 23:59:59'`;

  const nmAcoEtt = `Portfolio ${destino} - Ação Estratégica ${String(numeroAcao).padStart(2, '0')}`;
  const deAcoEtt = gerarLoremIpsum().replace(/\n/g, ' ').replace(/'/g, "''");
  
  // Regra: Histórico gerado dinamicamente terminando na hora da execução
  const deHst = valoresFixos?.deHst ?? gerarTextoHistorico();

  return `
    INSERT INTO [dbo].[T696ACES] 
        ([SQ_PTF_ACO], [SQ_ORI_ACO], [CD_UND], [CD_UND_APR], [NM_ACO_ETT], [DE_ACO_ETT], [ST_ACO_ETT], [TP_ACO_ETT], [MT_USU_ALT], [NM_USU_ALT], [MT_USU_RSP], [DH_LIM], [DE_HST], [DH_ALT])
    VALUES 
        (${sqPtfAco}, ${sqOriAco}, ${cdUnd}, ${cdUndApr}, '${nmAcoEtt}', '${deAcoEtt}', ${stAcoEtt}, ${tpAcoEtt}, '${mtUsuAlt}', '${nmUsuAlt}', '${mtUsuRsp}', ${dhLim}, '${deHst}', GETDATE());
  `;
}