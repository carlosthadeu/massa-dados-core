/**
 * Gera o cabeçalho descritivo para o script SQL.
 * 
 * @param destino - ID do portfólio de destino
 */
export function gerarCabecalho(destino: number): string {
  const dataFormatada = new Date().toLocaleString('pt-BR');
  return `-- =============================================
-- Script de Carga de Massa de Dados
-- Portfólio Destino: ${destino}
-- Data de Geração: ${dataFormatada}
-- =============================================`;
}

/**
 * Gera texto aleatório no formato Lorem Ipsum.
 */
export function gerarLoremIpsum(): string {
  return 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.';
}

/**
 * Sorteia uma quantidade de meses garantindo que a distância entre
 * a primeira entrega e a última NÃO seja superior a 6 meses.
 * 
 * @param quantidade - Quantidade de entregas a gerar (mínimo 1, máximo 6)
 */
export function gerarMeses(quantidade: number): number[] {
  if (quantidade <= 0) return [];
  
  // Limita a quantidade entre 1 e 6 para garantir que caiba no intervalo de 6 meses
  const totalEntregas = Math.min(quantidade, 6);
  
  // A janela total permitida é de no máximo 6 meses (ex: do mês 3 ao 8, diff = 5)
  const tamanhoJanela = 6; 
  
  // Sorteia o mês inicial possível para caber a janela (entre 1 e 12 - 6 + 1 = 7)
  const mesInicial = Math.floor(Math.random() * (12 - tamanhoJanela + 1)) + 1;
  const mesFinalMaximo = mesInicial + tamanhoJanela - 1;

  // Monta o pool de meses permitidos dentro da janela de 6 meses
  const mesesPermitidos: number[] = [];
  for (let m = mesInicial; m <= mesFinalMaximo; m++) {
    mesesPermitidos.push(m);
  }

  // Sorteia a quantidade solicitada de meses sem repetição dentro do intervalo permitido
  const selecionados: number[] = [];
  for (let i = 0; i < totalEntregas; i++) {
    if (mesesPermitidos.length === 0) break;
    const indexAleatorio = Math.floor(Math.random() * mesesPermitidos.length);
    selecionados.push(mesesPermitidos.splice(indexAleatorio, 1)[0]);
  }

  // Retorna os meses ordenados em ordem cronológica
  return selecionados.sort((a, b) => a - b);
}

/**
 * Gera percentuais aleatórios inteiros maiores que zero cuja soma total resulte em 100.
 * 
 * @param quantidade - Quantidade de partes para dividir
 */
export function gerarPercentuais(quantidade: number): number[] {
  if (quantidade <= 0) return [];
  if (quantidade === 1) return [100];

  const percentuais: number[] = [];
  let soma = 0;

  for (let i = 0; i < quantidade; i++) {
    const val = Math.floor(Math.random() * 10) + 1;
    percentuais.push(val);
    soma += val;
  }

  let somaAjustada = 0;
  for (let i = 0; i < quantidade - 1; i++) {
    percentuais[i] = Math.max(1, Math.round((percentuais[i] / soma) * 100));
    somaAjustada += percentuais[i];
  }
  percentuais[quantidade - 1] = 100 - somaAjustada;

  return percentuais;
}
