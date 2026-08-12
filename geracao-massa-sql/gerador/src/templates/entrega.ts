export interface EntregaValoresFixos {
  stEntAco?: number;
  mtUsuAlt?: string;
  nmUsuAlt?: string;
  [key: string]: any;
}

/**
 * Gera o INSERT para uma Entrega na tabela T696ENAC.
 *
 * @param destino - ID do portfólio de destino
 * @param numeroAcao - Número sequencial da ação
 * @param numeroEtapa - Número sequencial da etapa
 * @param numeroEntrega - Número sequencial da entrega
 * @param mes - Mês da entrega (MM_ENT)
 * @param percentual - Percentual planejado da entrega (PC_PLJ_ENT)
 * @param valoresFixos - Objeto opcional com valores fixos
 */
export function gerarInsertEntrega(
  destino: number,
  numeroAcao: number,
  numeroEtapa: number,
  numeroEntrega: number,
  mes: number,
  percentual: number,
  valoresFixos?: EntregaValoresFixos
): string {
  const stEntAco = valoresFixos?.stEntAco ?? 1;
  
  // Regra da Entrega: F856803 -> Facilitador 1681
  const mtUsuAlt = valoresFixos?.mtUsuAlt ?? 'F856803';
  const nmUsuAlt = valoresFixos?.nmUsuAlt ?? (mtUsuAlt === 'F856803' ? 'Facilitador 1681' : 'Equipe do Ambiente de Planejamento');

  return `
    INSERT INTO [dbo].[T696ENAC] 
        ([SQ_ETP_ACO], [MM_ENT], [PC_PLJ_ENT], [ST_ENT_ACO], [DH_ALT], [MT_USU_ALT], [NM_USU_ALT])
    VALUES 
        ((SELECT ID FROM #TempIDs WHERE Tipo = 'ETP'), ${mes}, ${percentual}, ${stEntAco}, GETDATE(), '${mtUsuAlt}', '${nmUsuAlt}');
  `;
}