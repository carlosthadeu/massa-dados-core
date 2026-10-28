import { mkdtemp, readFile, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { parseInstruction } from '../src/instructions/parser.js';
import { runCli } from '../src/cli/index.js';

const examplePath = path.resolve(
  __dirname,
  '../project-docs/cvs-exemplo/Portfolio analise diretoria.txt',
);

describe('Portuguese instruction parser', () => {
  it('extracts portfolio name, counts, and independent scheduling flags', () => {
    expect(
      parseInstruction(
        'Crie um portfólio chamado "Diretoria 2026" com 5 ações, 2 etapas por ação para o próximo ano e uma etapa atrasada.',
      ),
    ).toEqual({
      options: {
        actionsCount: 5,
        stepsPerAction: 2,
        portfolioName: 'Diretoria 2026',
        ensureNextYearStep: true,
        ensureDelayedStep: true,
      },
      clarifications: [],
    });
  });

  it('flags vague counts and per-unit counts for clarification', () => {
    expect(
      parseInstruction('Gere algumas ações e várias etapas.').clarifications,
    ).toEqual([{ kind: 'actionsCount' }, { kind: 'stepsPerAction' }]);
    expect(
      parseInstruction('Gere 4 ações por unidade.').clarifications,
    ).toEqual([{ kind: 'actionsPerUnit', count: 4 }]);
  });

  it('accepts the nextYear flag token and single-quoted portfolio names', () => {
    expect(
      parseInstruction("Crie um portfólio chamado 'Diretoria' para nextYear.")
        .options,
    ).toEqual({
      portfolioName: 'Diretoria',
      ensureNextYearStep: true,
    });
  });
});

describe('CLI natural-language flow', () => {
  it('generates SQL from an instruction without persisting the CSV template', async () => {
    const outputDirectory = await mkdtemp(
      path.join(os.tmpdir(), 'agente-s696-cli-'),
    );
    const outputPath = path.join(outputDirectory, 'instruction.sql');
    try {
      await runCli([
        '--csv',
        examplePath,
        '--author',
        'Test author',
        '--instr',
        'Crie um portfólio chamado "Diretoria 2026" com 5 ações, 2 etapas por ação para o próximo ano e uma etapa atrasada.',
        '--output',
        outputPath,
      ]);
      const script = await readFile(outputPath, 'utf8');
      expect(script).toContain('Diretoria 2026');
      expect(script).toContain('Etapa atrasada');
      expect(script).toContain('Próximo ano');
    } finally {
      await rm(outputDirectory, { recursive: true, force: true });
    }
  });

  it('returns an actionable error for ambiguous non-interactive instructions', async () => {
    await expect(
      runCli([
        '--csv',
        examplePath,
        '--author',
        'Test author',
        '--instr',
        'Gere algumas ações.',
      ]),
    ).rejects.toThrow('Specify the number of actions.');
  });
});
