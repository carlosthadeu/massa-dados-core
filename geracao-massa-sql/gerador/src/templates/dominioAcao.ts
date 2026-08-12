/**
 * Gera o INSERT para a tabela T696DOAC vinculando a ação a um domínio sorteado.
 * 
 * @param sqAcoEttExpr - Expressão SQL ou ID para buscar o SQ_ACO_ETT (ex: '(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')')
 * @param sqDmnForcado - Opcional: passa um domínio fixo caso deseje sobrescrever o sorteio
 */
export function gerarInsertDominioAcao(
  sqAcoEttExpr: string,
  sqDmnForcado?: number
): string {
  const dominiosDisponiveis = [143, 144, 145, 146];
  
  // Sorteia um dos 4 domínios se não for passado um valor fixo
  const sqDmn = sqDmnForcado ?? dominiosDisponiveis[Math.floor(Math.random() * dominiosDisponiveis.length)];

  return `
    INSERT INTO [dbo].[T696DOAC] 
        ([SQ_ACO_ETT], [SQ_DMN]) 
    VALUES 
        (${sqAcoEttExpr}, ${sqDmn});
  `;
}