import { gerarLoremIpsum } from '../utils/sql.js';

export interface EtapaValoresFixos {
  stEtpAco?: number;
  pcReaTot?: number;
  stRep?: number;
  mtUsuAlt?: string;
  nmUsuAlt?: string;
  [key: string]: any;
}

export function gerarInsertEtapa(
  destino: number,
  numeroAcao: number,
  numeroEtapa: number,
  cdUndCalculado: number,
  valoresFixos?: EtapaValoresFixos,
  anoRealizacao?: number
): string {
  const stEtpAco = valoresFixos?.stEtpAco ?? 1;
  const pcReaTot = valoresFixos?.pcReaTot ?? 0;
  const stRep = valoresFixos?.stRep ?? 0;
  
  const mtUsuAlt = valoresFixos?.mtUsuAlt ?? 'F856803';
  const nmUsuAlt = valoresFixos?.nmUsuAlt ?? (mtUsuAlt === 'F856803' ? 'Facilitador 1681' : 'Equipe do Ambiente de Planejamento');

  const nomeEtapa = `Portfolio ${destino} - Ação Estratégica ${String(numeroAcao).padStart(2, '0')} - Etapa ${String(numeroEtapa).padStart(2, '0')}`;
  const aaRea = anoRealizacao ?? new Date().getFullYear();
  const descricaoEtapa = gerarLoremIpsum().replace(/\n/g, ' ').replace(/'/g, "''");

  return `
    INSERT INTO [dbo].[T696ETAC] 
        ([SQ_ACO_ETT], [CD_UND], [DE_ETP_ACO], [DE_ENT_ETP_ACO], [DH_ALT], [MT_USU_ALT], [NM_USU_ALT], [ST_ETP_ACO], [AA_REA], [PC_REA_TOT], [ST_REP])
    VALUES 
        ((SELECT ID FROM #TempIDs WHERE Tipo = 'ACO'), ${cdUndCalculado}, '${nomeEtapa}', '${descricaoEtapa}', GETDATE(), '${mtUsuAlt}', '${nmUsuAlt}', ${stEtpAco}, ${aaRea}, ${pcReaTot}, ${stRep});
  `;
}