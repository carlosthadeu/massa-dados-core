import { readFileSync } from 'node:fs';
import path from 'node:path';
import { parseCsvContent } from '../src/csv/parser.js';
import { buildPreview } from '../src/generator/previewBuilder.js';
import { buildApplyScript } from '../src/generator/sqlBuilder.js';
import { applyTransform } from '../src/generator/transforms.js';
import { inferTemplate } from '../src/infer/inferTemplate.js';
import { validateTemplate } from '../src/validation/templateValidator.js';

const examplePath = path.resolve(
  __dirname,
  '../project-docs/cvs-exemplo/Portfolio analise diretoria.txt',
);
const template = inferTemplate(
  parseCsvContent(readFileSync(examplePath, 'utf8')),
  'Test author',
);

describe('phase 2 preview and transforms', () => {
  it('creates an exact preview with simulated IDs and remapped relationships', () => {
    const preview = buildPreview(template, {
      actionsCount: 5,
      stepsPerAction: 2,
      ensureDelayedStep: true,
    });
    const actions = preview.datasets.find(
      (dataset) => dataset.key === 'actions',
    );
    const stages = preview.datasets.find((dataset) => dataset.key === 'stages');
    const deliveries = preview.datasets.find(
      (dataset) => dataset.key === 'deliveries',
    );

    expect(preview.simulatedIds).toBe(true);
    expect(preview.parameters).toEqual({
      actionsCount: 5,
      stepsPerAction: 2,
      ensureNextYearStep: false,
      ensureDelayedStep: true,
      portfolioName: null,
    });
    expect(actions?.records).toHaveLength(5);
    expect(stages?.records).toHaveLength(15);
    expect(deliveries?.records.length).toBeGreaterThan(0);
    expect(actions?.records.map((record) => record.SQ_PTF_ACO)).toEqual(
      Array(5).fill('1'),
    );
    expect(
      stages?.records.every((record) =>
        actions?.records.some(
          (action) => action.SQ_ACO_ETT === record.SQ_ACO_ETT,
        ),
      ),
    ).toBe(true);
    expect(
      stages?.records.some((record) => record.DE_ETP_ACO?.includes('atrasada')),
    ).toBe(true);
    expect(
      stages?.records.some((record) =>
        record.DE_ETP_ACO?.includes('Próximo ano'),
      ),
    ).toBe(false);
    const sql = buildApplyScript(template, preview.datasets);
    expect(sql).toContain('INSERT INTO "T696ACES"');
    expect(sql).toContain('"SQ_ACO_ETT":"1"');
    expect(sql).toContain('Missing generated parent ID');
    expect(sql).toContain('INTO v_generated_id USING v_record');
  });

  it('applies date, default, and value mapping transforms with validation', () => {
    expect(
      applyTransform('2026-10-26 12:25:27.807', { type: 'timestamp' }),
    ).toBe('2026-10-26 12:25:27.807');
    expect(applyTransform(null, { type: 'default', value: 'fallback' })).toBe(
      'fallback',
    );
    expect(applyTransform(null, { type: 'default', value: null })).toBeNull();
    expect(
      applyTransform('10', {
        type: 'valueMap',
        values: { '10': '10', '20': '20' },
        fallback: '10',
      }),
    ).toBe('10');
    expect(() =>
      applyTransform('2026-02-30', { type: 'date', format: 'YYYY-MM-DD' }),
    ).toThrow('Invalid date value');
  });

  it('creates next-year and overdue stages as distinct scheduling options', () => {
    const sourceYear = Number(
      template.datasets.find((dataset) => dataset.key === 'stages')?.records[0]
        ?.AA_REA,
    );
    const preview = buildPreview(template, {
      actionsCount: 1,
      stepsPerAction: 1,
      ensureNextYearStep: true,
      ensureDelayedStep: true,
      portfolioName: 'Portfolio conversacional',
    });
    const portfolio = preview.datasets.find(
      (dataset) => dataset.key === 'portfolio',
    );
    const stages = preview.datasets.find((dataset) => dataset.key === 'stages');

    expect(portfolio?.records[0]?.NM_PTF).toBe('Portfolio conversacional');
    expect(stages?.records).toHaveLength(3);
    expect(stages?.records.map((record) => Number(record.AA_REA))).toEqual([
      sourceYear,
      sourceYear + 1,
      sourceYear - 1,
    ]);
  });

  it('validates the inferred template and rejects invalid stage bounds', () => {
    expect(() => validateTemplate(template)).not.toThrow();
    const invalid = structuredClone(template);
    invalid.rules.stagesPerAction.minimum = 6;
    expect(() => validateTemplate(invalid)).toThrow(
      'minimum stages per action exceeds maximum',
    );
    expect(() => buildPreview(template, { actionsCount: 0 })).toThrow(
      'actionsCount must be an integer between 1 and 1000',
    );
  });
});
