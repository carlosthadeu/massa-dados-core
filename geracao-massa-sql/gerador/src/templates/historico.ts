/**
 * Gera o texto formatado para a coluna DE_HST com horários calculados retroativamente
 * a partir da data/hora atual (GETDATE()), mantendo intervalos de minutos entre os passos.
 * 
 * @param dataHoraReferencia - Data/Hora base (padrão: new Date())
 */
export function gerarTextoHistorico(dataHoraReferencia: Date = new Date()): string {
  // O último histórico ("Aprovar Proposta de Ação" do Superintendente) é a hora atual
  const data6 = new Date(dataHoraReferencia);

  // Retrocede os horários em minutos/segundos aproximados (ex: ~3 min de diferença)
  const data5 = new Date(data6.getTime() - 2 * 60 * 1000 - 50 * 1000);  // ~3 min antes
  const data4 = new Date(data5.getTime() - 15 * 60 * 1000 - 48 * 1000); // ~15 min antes
  const data3 = new Date(data4.getTime() - 1 * 60 * 1000 - 37 * 1000);  // ~2 min antes
  const data2 = new Date(data3.getTime() - 4 * 60 * 1000 - 32 * 1000);  // ~4 min antes
  const data1 = new Date(data2.getTime() - 16 * 1000);                  // ~16 seg antes

  const formatarData = (d: Date) => {
    const dia = String(d.getDate()).padStart(2, '0');
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const ano = d.getFullYear();
    const hora = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    const seg = String(d.getSeconds()).padStart(2, '0');
    return `${dia}/${mes}/${ano} ${hora}:${min}:${seg}`;
  };

  const historico = `Em: ${formatarData(data1)} 
Por: GERENTE PROD SERVIC-DIRGE III - F856806 
Ação:Concluir Edição da Proposta
___________________________

Em: ${formatarData(data2)} 
Por: SECRETARIA IV - F856803 
Ação:Aceitar e Planejar Proposta
___________________________

Em: ${formatarData(data3)} 
Por: SECRETARIA IV - F856803 
Ação:Concluir Planejamento de Ação
___________________________

Em: ${formatarData(data4)} 
Por: COORDENADOR TECNICO DE TI I - F856804 
Ação:Aprovar Proposta de Ação
___________________________

Em: ${formatarData(data5)} 
Por: GERENTE PROD SERVIC-DIRGE III - F856806 
Ação:Aprovar Proposta de Ação
___________________________

Em: ${formatarData(data6)} 
Por: SUPERINTENDENTE - F856807 
Ação:Aprovar Proposta de Ação
___________________________`;

  // Faz o escape de aspas simples para garantir validade no T-SQL
  return historico.replace(/'/g, "''");
}