import * as fs from 'fs';
import * as path from 'path';
import {
  gerarCabecalho,
  gerarMeses,
  gerarPercentuais
} from './utils/sql.js';
import { gerarInsertAcao } from './templates/acao.js';
import { gerarInsertEtapa } from './templates/etapa.js';
import { gerarInsertEntrega } from './templates/entrega.js';
import { gerarInsertVinculoAcao } from './templates/vinculoAcao.js';
import { gerarInsertDominioAcao } from './templates/dominioAcao.js';
import { GeradorParams } from './types.js';

function gerarBlocoEntregas(
  destino: number,
  numeroAcao: number,
  numeroEtapa: number,
  valoresFixos?: Record<string, any>
): string {
  let bloco = '';
  const numEntregas = Math.floor(Math.random() * 5) + 2;
  const meses = gerarMeses(numEntregas);
  const percentuais = gerarPercentuais(numEntregas);

  for (let k = 1; k <= numEntregas; k++) {
    bloco += gerarInsertEntrega(
      destino,
      numeroAcao,
      numeroEtapa,
      k,
      meses[k - 1],
      percentuais[k - 1],
      valoresFixos
    );
  }
  return bloco;
}

export function gerarScript(params: GeradorParams): string {
  const cabecalho = gerarCabecalho(params.destino);
  const quantidadeAcoes = params.quantidadeAcoes ?? 5;
  const minEtapas = params.minEtapas ?? 2;
  const maxEtapas = params.maxEtapas ?? 5;
  
  // Regra: Ano corrente pega do sistema (2026) e subsequente é anoAtual + 1 (2027)
  const anoAtual = new Date().getFullYear();

  let script = cabecalho;

  script += '\n\nBEGIN TRANSACTION;\n';
  script += 'BEGIN TRY\n';

  script += '    IF OBJECT_ID(\'tempdb..#TempIDs\') IS NOT NULL\n';
  script += '        DROP TABLE #TempIDs;\n\n';
  script += '    CREATE TABLE #TempIDs (\n';
  script += '        Tipo VARCHAR(10),\n';
  script += '        ID INT\n';
  script += '    );\n\n';

  script += '    -- Início da criação de ações, vínculos, domínios, etapas e entregas\n';

  const metadeAcoes = Math.ceil(quantidadeAcoes / 2);

  for (let i = 1; i <= quantidadeAcoes; i++) {
    const cdUndAcao = i <= metadeAcoes ? 1681 : 955;

    script += `\n    -- Ação ${i} (CD_UND: ${cdUndAcao})\n`;
    script += '    ' + gerarInsertAcao(params.destino, i, cdUndAcao, params.valoresFixos).trim() + '\n';
    script += '    DELETE FROM #TempIDs WHERE Tipo = \'ACO\';\n';
    script += '    INSERT INTO #TempIDs (Tipo, ID) VALUES (\'ACO\', SCOPE_IDENTITY());\n';

    script += '    ' + gerarInsertVinculoAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')', 58).trim() + '\n';
    script += '    ' + gerarInsertVinculoAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')', 60).trim() + '\n';

    script += '    ' + gerarInsertDominioAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')').trim() + '\n';

    if (i === 1) {
      // Etapa 1 -> Ano Atual (2026)
      script += '    ' + gerarInsertEtapa(params.destino, i, 1, cdUndAcao, params.valoresFixos, anoAtual).trim() + '\n';
      script += '    DELETE FROM #TempIDs WHERE Tipo = \'ETP\';\n';
      script += '    INSERT INTO #TempIDs (Tipo, ID) VALUES (\'ETP\', SCOPE_IDENTITY());\n';
      script += '    ' + gerarBlocoEntregas(params.destino, i, 1, params.valoresFixos).trim() + '\n';

      // Etapa 2 -> Ano Subsequente (2027)
      script += '    ' + gerarInsertEtapa(params.destino, i, 2, cdUndAcao, params.valoresFixos, anoAtual + 1).trim() + '\n';
      script += '    DELETE FROM #TempIDs WHERE Tipo = \'ETP\';\n';
      script += '    INSERT INTO #TempIDs (Tipo, ID) VALUES (\'ETP\', SCOPE_IDENTITY());\n';
      script += '    ' + gerarBlocoEntregas(params.destino, i, 2, params.valoresFixos).trim() + '\n';

      const limiteMaximoEtapasAcao1 = Math.max(3, maxEtapas);
      const totalEtapasAcao1 = Math.floor(Math.random() * (limiteMaximoEtapasAcao1 - 3 + 1)) + 3;

      for (let j = 3; j <= totalEtapasAcao1; j++) {
        // Correção: Sorteia unicamente entre 2026 e 2027 (anoAtual + 0 ou 1)
        const anoAleatorio = anoAtual + Math.floor(Math.random() * 2);
        script += '    ' + gerarInsertEtapa(params.destino, i, j, cdUndAcao, params.valoresFixos, anoAleatorio).trim() + '\n';
        script += '    DELETE FROM #TempIDs WHERE Tipo = \'ETP\';\n';
        script += '    INSERT INTO #TempIDs (Tipo, ID) VALUES (\'ETP\', SCOPE_IDENTITY());\n';
        script += '    ' + gerarBlocoEntregas(params.destino, i, j, params.valoresFixos).trim() + '\n';
      }
    } else {
      const numEtapas = Math.floor(Math.random() * (maxEtapas - minEtapas + 1)) + minEtapas;
      for (let j = 1; j <= numEtapas; j++) {
        // Correção: Sorteia unicamente entre 2026 e 2027 (anoAtual + 0 ou 1)
        const anoAleatorio = anoAtual + Math.floor(Math.random() * 2);
        script += '    ' + gerarInsertEtapa(params.destino, i, j, cdUndAcao, params.valoresFixos, anoAleatorio).trim() + '\n';
        script += '    DELETE FROM #TempIDs WHERE Tipo = \'ETP\';\n';
        script += '    INSERT INTO #TempIDs (Tipo, ID) VALUES (\'ETP\', SCOPE_IDENTITY());\n';
        script += '    ' + gerarBlocoEntregas(params.destino, i, j, params.valoresFixos).trim() + '\n';
      }
    }
  }

  script += '\n    COMMIT TRANSACTION;\n';
  script += '    PRINT \'Carga de massa de dados realizada com sucesso.\';\n';
  script += 'END TRY\n';
  
  script += 'BEGIN CATCH\n';
  script += '    IF OBJECT_ID(\'tempdb..#TempIDs\') IS NOT NULL\n';
  script += '        DROP TABLE #TempIDs;\n';
  script += '    IF @@TRANCOUNT > 0\n';
  script += '        ROLLBACK TRANSACTION;\n';
  script += '    PRINT \'Erro ao executar a carga de dados. Operação cancelada.\';\n';
  script += '    THROW;\n';
  script += 'END CATCH;\n';

  return script;
}

const args = process.argv.slice(2);
if (args.length > 0) {
  const destino = parseInt(args[0], 10);
  const quantidadeAcoes = args[1] ? parseInt(args[1], 10) : 5;

  if (isNaN(destino)) {
    console.error('Erro: O parâmetro <destino> deve ser um número.');
    console.log('Uso: node dist/index.js <destino> [quantidadeAcoes]');
    process.exit(1);
  }

  const scriptResult = gerarScript({ destino, quantidadeAcoes });

  const outputDir = path.resolve(process.cwd(), 'output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  const outputFilePath = path.join(outputDir, `massa_dados_ptf_${destino}.sql`);
  fs.writeFileSync(outputFilePath, scriptResult, 'utf-8');

  console.log(`Script SQL gerado com sucesso em: ${outputFilePath}`);
} else {
  console.log('Uso: node dist/index.js <destino> [quantidadeAcoes]');
  console.log('Exemplo: node dist/index.js 78 10');
}