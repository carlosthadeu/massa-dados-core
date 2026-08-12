/**
 * Interface de linha de comando (CLI) do gerador de massa de dados SQL.
 *
 * Responsável por ler os argumentos da linha de comando, validar a entrada
 * e orquestrar a geração e persistência do script SQL gerado.
 *
 * Uso:
 * ```
 * node dist/cli.js <destino> [quantidadeAcoes]
 * ```
 *
 * Exemplo:
 * ```
 * node dist/cli.js 78 10
 * ```
 *
 * @module cli
 */
import * as fs from 'fs';
import * as path from 'path';
import { gerarScript } from './index.js';

/**
 * Obtém o código do portfólio de destino a partir dos argumentos da CLI.
 *
 * @param arg - Primeiro argumento da linha de comando ('<destino>')
 * @returns O código do portfólio de destino como número
 */
function obterDestino(arg: string): number {
  const destino = parseInt(arg, 10);
  if (isNaN(destino)) {
    console.error('Erro: O parâmetro <destino> deve ser um número.');
    console.log('Uso: node dist/cli.js <destino> [quantidadeAcoes]');
    process.exit(1);
  }
  return destino;
}

/**
 * Obtém a quantidade de ações a gerar a partir dos argumentos da CLI.
 *
 * @param arg - Segundo argumento da linha de comando ('[quantidadeAcoes]', opcional)
 * @returns A quantidade de ações a gerar (padrão: 5)
 */
function obterQuantidadeAcoes(arg?: string): number {
  return arg ? parseInt(arg, 10) : 5;
}

/**
 * Persiste o script SQL gerado no diretório `output/`.
 *
 * @param script - Conteúdo SQL gerado
 * @param destino - Código do portfólio de destino (usado no nome do arquivo)
 */
function salvarScript(script: string, destino: number): void {
  const outputDir = path.resolve(process.cwd(), 'output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  const outputFilePath = path.join(outputDir, 'massa_dados_ptf_' + destino + '.sql');
  fs.writeFileSync(outputFilePath, script, 'utf-8');

  console.log('Script SQL gerado com sucesso em: ' + outputFilePath);
}

/**
 * Exibe a mensagem de ajuda com a sintaxe de utilização da CLI.
 */
function exibirAjuda(): void {
  console.log('Uso: node dist/cli.js <destino> [quantidadeAcoes]');
  console.log('Exemplo: node dist/cli.js 78 10');
}

/**
 * Ponto de entrada principal da CLI.
 *
 * Lê os argumentos, gera o script e salva em arquivo. Se nenhum argumento
 * for informado, exibe a ajuda.
 */
function main(): void {
  const args = process.argv.slice(2);

  if (args.length === 0) {
    exibirAjuda();
    return;
  }

  const destino = obterDestino(args[0]);
  const quantidadeAcoes = obterQuantidadeAcoes(args[1]);

  const scriptResult = gerarScript({ destino, quantidadeAcoes });
  salvarScript(scriptResult, destino);
}

main();