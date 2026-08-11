// file: geracao-massa-sql/gerador/tests/index.test.ts
import { describe, it, expect } from 'vitest';
import { gerarScript } from '../src/index.js';
import { gerarCabecalho, gerarLoremIpsum, gerarMeses, gerarPercentuais } from '../src/utils/sql.js';
import { gerarInsertAcao } from '../src/templates/acao.js';
import { gerarInsertEtapa } from '../src/templates/etapa.js';
import { gerarInsertEntrega } from '../src/templates/entrega.js';
import { gerarInsertVinculoAcao } from '../src/templates/vinculoAcao.js';
import { gerarInsertDominioAcao } from '../src/templates/dominioAcao.js';
import { gerarTextoHistorico } from '../src/templates/historico.js';

// =============================================================================
// TESTES DE UTILS — src/utils/sql.ts
// =============================================================================

describe('gerarCabecalho()', () => {
  it('deve incluir o destino informado no cabeçalho', () => {
    const cabecalho = gerarCabecalho(78);
    expect(cabecalho).toContain('78');
  });

  it('deve conter a marcação de script de carga', () => {
    const cabecalho = gerarCabecalho(99);
    expect(cabecalho).toContain('Script de Carga de Massa de Dados');
  });
});

describe('gerarLoremIpsum()', () => {
  it('deve retornar uma string não vazia', () => {
    expect(gerarLoremIpsum().length).toBeGreaterThan(0);
  });

  it('deve conter as palavras lorem e ipsum', () => {
    const texto = gerarLoremIpsum();
    expect(texto).toContain('Lorem');
    expect(texto).toContain('ipsum');
  });
});

describe('gerarMeses()', () => {
  it('deve limitar a quantidade ao intervalo de 1 a 6', () => {
    // Pedindo 10, deve retornar no máximo 6 meses
    expect(gerarMeses(10).length).toBe(6);
  });

  it('deve retornar lista vazia para quantidade 0 ou negativa', () => {
    expect(gerarMeses(0)).toEqual([]);
    expect(gerarMeses(-3)).toEqual([]);
  });

  it('deve retornar meses entre 1 e 12', () => {
    const meses = gerarMeses(5);
    meses.forEach((mes) => {
      expect(mes).toBeGreaterThanOrEqual(1);
      expect(mes).toBeLessThanOrEqual(12);
    });
  });

  it('deve retornar meses únicos e ordenados', () => {
    const meses = gerarMeses(5);
    const unicos = new Set(meses);
    expect(unicos.size).toBe(meses.length);
    expect(meses).toEqual([...meses].sort((a, b) => a - b));
  });

  it('deve garantir intervalo máximo de 6 meses entre o primeiro e o último', () => {
    const meses = gerarMeses(5);
    if (meses.length >= 2) {
      const primeiro = meses[0];
      const ultimo = meses[meses.length - 1];
      expect(ultimo - primeiro).toBeLessThanOrEqual(6);
    }
  });
});

describe('gerarPercentuais()', () => {
  it('deve retornar lista vazia para quantidade 0 ou negativa', () => {
    expect(gerarPercentuais(0)).toEqual([]);
    expect(gerarPercentuais(-1)).toEqual([]);
  });

  it('deve retornar [100] quando a quantidade for 1', () => {
    expect(gerarPercentuais(1)).toEqual([100]);
  });

  it('deve gerar percentuais cuja soma seja 100', () => {
    const percentuais = gerarPercentuais(5);
    const soma = percentuais.reduce((acc, curr) => acc + curr, 0);
    expect(soma).toBe(100);
  });

  it('deve gerar percentuais positivos', () => {
    const percentuais = gerarPercentuais(4);
    percentuais.forEach((valor) => {
      expect(valor).toBeGreaterThan(0);
    });
  });
});

// =============================================================================
// TESTES DE TEMPLATES — src/templates/*.ts
// =============================================================================

describe('gerarInsertAcao()', () => {
  it('deve conter a tabela T696ACES', () => {
    const sql = gerarInsertAcao(78, 1, 1681);
    expect(sql).toContain('T696ACES');
  });

  it('deve conter o destino e o nome padronizado da ação', () => {
    const sql = gerarInsertAcao(78, 1, 1681);
    expect(sql).toContain('78');
    expect(sql).toContain('Portfolio 78 - Ação Estratégica 01');
  });

  it('deve usar CD_UND informado como padrão quando valoresFixos não é fornecido', () => {
    const sql = gerarInsertAcao(78, 1, 1681);
    expect(sql).toContain('1681');
  });

  it('deve respeitar o CD_UND de valoresFixos quando informado', () => {
    const sql = gerarInsertAcao(78, 1, 1681, { cdUnd: 955 });
    expect(sql).toContain('955');
  });
});

describe('gerarInsertEtapa()', () => {
  it('deve conter a tabela T696ETAC', () => {
    const sql = gerarInsertEtapa(78, 1, 1, 1681);
    expect(sql).toContain('T696ETAC');
  });

  it('deve conter o nome padronizado da etapa', () => {
    const sql = gerarInsertEtapa(78, 1, 1, 1681);
    expect(sql).toContain('Portfolio 78 - Ação Estratégica 01 - Etapa 01');
  });

  it('deve usar o ano de realização informado', () => {
    const sql = gerarInsertEtapa(78, 1, 1, 1681, undefined, 2027);
    expect(sql).toContain('2027');
  });

  it('deve usar o ano corrente quando anoRealizacao não é informado', () => {
    const anoAtual = new Date().getFullYear();
    const sql = gerarInsertEtapa(78, 1, 1, 1681);
    expect(sql).toContain(String(anoAtual));
  });
});

describe('gerarInsertEntrega()', () => {
  it('deve conter a tabela T696ENAC', () => {
    const sql = gerarInsertEntrega(78, 1, 1, 1, 3, 25);
    expect(sql).toContain('T696ENAC');
  });

  it('deve conter mês e percentual informados', () => {
    const sql = gerarInsertEntrega(78, 1, 1, 1, 3, 30);
    expect(sql).toContain('3');
    expect(sql).toContain('30');
  });
});

describe('gerarInsertVinculoAcao()', () => {
  it('deve conter a tabela T696VIAC', () => {
    const sql = gerarInsertVinculoAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')', 58);
    expect(sql).toContain('T696VIAC');
  });

  it('deve conter a orientação estratégica informada', () => {
    const sql = gerarInsertVinculoAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')', 60);
    expect(sql).toContain('60');
  });
});

describe('gerarInsertDominioAcao()', () => {
  it('deve conter a tabela T696DOAC', () => {
    const sql = gerarInsertDominioAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')', 143);
    expect(sql).toContain('T696DOAC');
  });

  it('deve respeitar o domínio forçado quando informado', () => {
    const sql = gerarInsertDominioAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')', 145);
    expect(sql).toContain('145');
  });

  it('deve sortear domínio entre os valores permitidos quando não informado', () => {
    const sql = gerarInsertDominioAcao('(SELECT ID FROM #TempIDs WHERE Tipo = \'ACO\')');
    const dominios = [143, 144, 145, 146];
    const encontrado = dominios.some((dominio) => sql.includes(String(dominio)));
    expect(encontrado).toBe(true);
  });
});

describe('gerarTextoHistorico()', () => {
  it('deve conter as 6 etapas do fluxo de aprovação', () => {
    const historico = gerarTextoHistorico(new Date('2026-08-10T12:00:00'));
    expect(historico).toContain('Concluir Edição da Proposta');
    expect(historico).toContain('Aceitar e Planejar Proposta');
    expect(historico).toContain('Concluir Planejamento de Ação');
    expect(historico).toContain('Aprovar Proposta de Ação');
  });

  it('deve conter os 4 perfis envolvidos', () => {
    const historico = gerarTextoHistorico(new Date('2026-08-10T12:00:00'));
    expect(historico).toContain('GERENTE PROD SERVIC-DIRGE III - F856806');
    expect(historico).toContain('SECRETARIA IV - F856803');
    expect(historico).toContain('COORDENADOR TECNICO DE TI I - F856804');
    expect(historico).toContain('SUPERINTENDENTE - F856807');
  });

  it('deve conter a data de referência formatada', () => {
    const historico = gerarTextoHistorico(new Date('2026-08-10T12:00:00'));
    expect(historico).toContain('10/08/2026');
  });
});

// =============================================================================
// TESTES DA ORQUESTRAÇÃO — src/index.ts
// =============================================================================

describe('gerarScript()', () => {
  it('deve conter BEGIN TRANSACTION e COMMIT', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 1 });
    expect(script).toContain('BEGIN TRANSACTION');
    expect(script).toContain('COMMIT TRANSACTION');
  });

  it('deve conter a tabela temporária #TempIDs', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 1 });
    expect(script).toContain('#TempIDs');
  });

  it('deve gerar INSERTs para todas as 5 tabelas-alvo', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 1 });
    expect(script).toContain('T696ACES');
    expect(script).toContain('T696VIAC');
    expect(script).toContain('T696DOAC');
    expect(script).toContain('T696ETAC');
    expect(script).toContain('T696ENAC');
  });

  it('deve gerar a quantidade exata de ações solicitada (quantidadeAcoes = 3)', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 3 });
    const ocorrencias = (script.match(/INSERT INTO \[dbo\]\.\[T696ACES\]/g) || []).length;
    expect(ocorrencias).toBe(3);
  });

  it('deve gerar 2 vínculos por ação (SQ_ORT_ETT = 58 e 60)', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 1 });
    const vinculos = (script.match(/INSERT INTO \[dbo\]\.\[T696VIAC\]/g) || []).length;
    expect(vinculos).toBe(2);
  });

  it('deve gerar pelo menos 2 etapas para a primeira ação', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 1 });
    const etapas = (script.match(/INSERT INTO \[dbo\]\.\[T696ETAC\]/g) || []).length;
    expect(etapas).toBeGreaterThanOrEqual(2);
  });

  it('deve incluir a mensagem de sucesso no final', () => {
    const script = gerarScript({ destino: 78, quantidadeAcoes: 1 });
    expect(script).toContain('Carga de massa de dados realizada com sucesso.');
  });
});