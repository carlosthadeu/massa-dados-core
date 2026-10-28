import { readFileSync } from 'node:fs';
import path from 'node:path';
import { parseCsvContent } from '../src/csv/parser.js';
import { inferTemplate } from '../src/infer/inferTemplate.js';
import { buildApplyScript } from '../src/generator/sqlBuilder.js';

const examplePath = path.resolve(
  __dirname,
  '../project-docs/cvs-exemplo/Portfolio analise diretoria.txt',
);
const exampleContent = readFileSync(examplePath, 'utf8');

describe('phase 1 portfolio example', () => {
  const datasets = parseCsvContent(exampleContent);
  const template = inferTemplate(datasets, 'Test author');

  it('parses all six CSV sections with multiline fields', () => {
    expect(datasets.map((dataset) => dataset.key)).toEqual([
      'portfolio',
      'actions',
      'stages',
      'actionLinks',
      'actionDomains',
      'deliveries',
    ]);
    expect(
      datasets.find((dataset) => dataset.key === 'actions')?.records,
    ).toHaveLength(8);
    expect(
      datasets.find((dataset) => dataset.key === 'actions')?.records[0].DE_HST,
    ).toContain('Aprovar Proposta de Ação');
  });

  it('infers eight actions, four per unit, all in Analysis Directorate status', () => {
    expect(template.rules.actionCount).toBe(8);
    expect(template.rules.actionsByUnit).toEqual({ '955': 4, '1681': 4 });
    expect(template.rules.actionStatus).toEqual({
      column: 'ST_ACO_ETT',
      value: '10',
      label: 'Análise Diretoria',
    });
    expect(template.rules.stagesPerAction).toEqual({ minimum: 3, maximum: 5 });
    expect(template.fieldMaps.length).toBeGreaterThanOrEqual(3);
  });

  it('builds a PostgreSQL DO script that remaps generated relationship IDs', () => {
    const script = buildApplyScript(template);
    expect(script).toContain('DO $portfolio$');
    expect(script).toContain('INSERT INTO "T696POAC"');
    expect(script).toContain('INSERT INTO "T696ACES"');
    expect(script).toContain('INSERT INTO "T696ETAC"');
    expect(script).toContain('INSERT INTO "T696VIAC"');
    expect(script).toContain('INSERT INTO "T696DOAC"');
    expect(script).toContain('INSERT INTO "T696ENAC"');
    expect(script).toContain("v_record := v_record - 'SQ_ACO_ETT'");
    expect(script).toContain("v_id_map ->> format('portfolio:%s'");
    expect(script).toContain('RETURNING "SQ_ACO_ETT"');
    expect(script).toContain('COMMIT;');
  });
});
