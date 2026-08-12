/**
 * Gera o INSERT para a tabela de vínculo da ação (T696VIAC).
 */
export function gerarInsertVinculoAcao(
  sqAcoEtt: string | number = '(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')',
  sqOrtEtt: number
): string {
  return `
    INSERT INTO [dbo].[T696VIAC] 
        ([SQ_ACO_ETT], [SQ_ORT_ETT]) 
    VALUES 
        (${sqAcoEtt}, ${sqOrtEtt});
  `;
}