/**
 * Script de teste manual e verificação das rotinas de geração de SQL.
 *
 * Funcionalidades:
 * - Demonstração de utilitários de geração de texto, meses e percentuais
 * - Validação da formatação do histórico
 * - Simulação da geração de instruções INSERT (Ação, Etapa, Entrega)
 * - Validação do script SQL completo
 *
 * @module TesteManual
 */

import { gerarScript } from '../src/index.js';
import { gerarLoremIpsum, gerarMeses, gerarPercentuais } from '../src/utils/sql.js';
import { gerarInsertAcao } from '../src/templates/acao.js';
import { gerarInsertEtapa } from '../src/templates/etapa.js';
import { gerarInsertEntrega } from '../src/templates/entrega.js';
import { gerarTextoHistorico } from '../src/templates/historico.js';

console.log('=== TESTE MANUAL DE GERAÇÃO ===\n');

// 1. Lorem Ipsum
console.log('--- Lorem Ipsum ---');
console.log(gerarLoremIpsum());
console.log('\n');

// 2. Meses
console.log('--- Meses (Quantidade: 4) ---');
console.log(gerarMeses(4));
console.log('\n');

// 3. Percentuais
const percentuais = gerarPercentuais(3);
console.log('--- Percentuais (Quantidade: 3) ---');
console.log(percentuais);
console.log('Soma total:', percentuais.reduce((a, b) => a + b, 0));
console.log('\n');

// 4. Histórico
console.log('--- Histórico (DE_HST) ---');
console.log(gerarTextoHistorico(new Date('2026-08-10T12:00:00')));
console.log('\n');

// 5. Simulação de INSERTs
console.log('--- Simulação de SQL Gerado ---');
console.log('Ação:');
console.log(gerarInsertAcao(78, 1, 1681));
console.log('Etapa:');
console.log(gerarInsertEtapa(78, 1, 1, 1681, undefined, 2026));
console.log('Entrega:');
console.log(gerarInsertEntrega(78, 1, 1, 1, 3, 25));
console.log('\n');

// 6. Script completo
console.log('--- Script SQL Completo (primeiras 500 caracteres) ---');
const script = gerarScript({ destino: 78, quantidadeAcoes: 2 });
console.log(script.substring(0, 500));