import { readFile, mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { createInterface } from 'node:readline/promises';
import { stdin, stdout } from 'node:process';
import { parseCsvContent } from '../csv/parser.js';
import { inferTemplate } from '../infer/inferTemplate.js';
import { buildApplyScript } from '../generator/sqlBuilder.js';
import { buildPreview } from '../generator/previewBuilder.js';
import { closePool } from '../persistence/db.js';
import {
  executeApplyScript,
  getTemplate,
  saveFieldMapVersion,
  saveTemplate,
} from '../persistence/templateRepo.js';
import { PreviewOptions, Template } from '../types.js';
import { parseInstruction } from '../instructions/parser.js';
import {
  isTransformSpec,
  validateTemplate,
} from '../validation/templateValidator.js';

interface CliOptions {
  csvPath?: string;
  author?: string;
  templateId?: string;
  params?: string;
  transforms?: string;
  instruction?: string;
  saveTemplate: boolean;
  generate: boolean;
  apply: boolean;
  outputPath?: string;
  previewPath?: string;
}

function parseArguments(args: string[]): CliOptions | undefined {
  if (args.length === 0 || args.includes('--help') || args.includes('-h')) {
    return undefined;
  }

  const values = new Map<string, string>();
  const flags = new Set<string>();
  const valueOptions = [
    '--csv',
    '--author',
    '--template-id',
    '--params',
    '--transforms',
    '--instr',
    '--output',
    '--preview',
  ];
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    if (argument === '--generate' || argument === '--apply') {
      flags.add(argument);
      continue;
    }
    if (argument === '--save-template') {
      flags.add(argument);
      continue;
    }
    if (!valueOptions.includes(argument)) {
      throw new Error(`Unknown argument: ${argument}`);
    }
    if (
      argument === '--preview' &&
      (!args[index + 1] || args[index + 1].startsWith('--'))
    ) {
      values.set(argument, 'true');
      continue;
    }
    const value = args[index + 1];
    if (!value || value.startsWith('--')) {
      throw new Error(`Missing value for ${argument}.`);
    }
    values.set(argument, value);
    index += 1;
  }

  const csvPath = values.get('--csv');
  const author = values.get('--author');
  const templateId = values.get('--template-id');
  if (templateId && (csvPath || author)) {
    throw new Error(
      'Use either --template-id or --csv with --author, not both.',
    );
  }
  if (!templateId && (!csvPath || !author)) {
    throw new Error(
      'Provide --template-id or both --csv <path> and --author <name>.',
    );
  }
  const apply = flags.has('--apply');
  const instruction = values.get('--instr');
  const generate = flags.has('--generate') || apply || Boolean(instruction);
  const previewPath = values.get('--preview');
  if (values.has('--params') && !previewPath && !generate) {
    throw new Error('--params requires --preview, --generate, or --apply.');
  }
  if (values.has('--transforms') && !previewPath && !generate) {
    throw new Error('--transforms requires --preview, --generate, or --apply.');
  }
  if (flags.has('--save-template') && templateId) {
    throw new Error('--save-template cannot be used with --template-id.');
  }
  return {
    csvPath,
    author,
    templateId,
    params: values.get('--params'),
    transforms: values.get('--transforms'),
    instruction,
    saveTemplate: flags.has('--save-template'),
    generate,
    apply,
    outputPath: values.get('--output'),
    previewPath,
  };
}

function printHelp(): void {
  console.log(
    [
      'Usage:',
      '  node dist/cli.js --csv <path> --author <name> [--preview [path]] [--generate] [--params <json-or-file>] [--output <sql-path>]',
      '  node dist/cli.js --template-id <id> [--preview [path]] [--generate] [--params <json-or-file>] [--output <sql-path>]',
      '  Add --transforms <json-or-file> to customize per-column transforms.',
      '  Add --instr "<instrução em português>" to derive preview parameters and generate SQL.',
      '  CSV preview is database-free; use --save-template to persist it.',
      '  Add --apply to execute generated SQL after confirming the exact portfolio name interactively.',
    ].join('\n'),
  );
}

async function loadParameters(source?: string): Promise<PreviewOptions> {
  if (!source) {
    return {};
  }
  let json = source;
  if (!source.trimStart().startsWith('{')) {
    json = await readFile(path.resolve(source), 'utf8');
  }
  json = json.replace(/^\uFEFF/, '');
  let value: unknown;
  try {
    value = JSON.parse(json);
  } catch (error) {
    throw new Error(
      'The --params value must be valid JSON or a path to a JSON file.',
      {
        cause: error,
      },
    );
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('The parameters JSON must be an object.');
  }
  const record = value as Record<string, unknown>;
  const allowed = new Set([
    'actionsCount',
    'stepsPerAction',
    'ensureDelayedStep',
    'ensureNextYearStep',
    'portfolioName',
  ]);
  const unknown = Object.keys(record).filter((key) => !allowed.has(key));
  if (unknown.length > 0) {
    throw new Error(`Unsupported parameter(s): ${unknown.join(', ')}.`);
  }
  if (
    record.actionsCount !== undefined &&
    (typeof record.actionsCount !== 'number' ||
      !Number.isInteger(record.actionsCount))
  ) {
    throw new Error('actionsCount must be an integer.');
  }
  if (
    record.stepsPerAction !== undefined &&
    (typeof record.stepsPerAction !== 'number' ||
      !Number.isInteger(record.stepsPerAction))
  ) {
    throw new Error('stepsPerAction must be an integer.');
  }
  if (
    record.ensureNextYearStep !== undefined &&
    typeof record.ensureNextYearStep !== 'boolean'
  ) {
    throw new Error('ensureNextYearStep must be a boolean.');
  }
  if (
    record.ensureDelayedStep !== undefined &&
    typeof record.ensureDelayedStep !== 'boolean'
  ) {
    throw new Error('ensureDelayedStep must be a boolean.');
  }
  if (
    record.portfolioName !== undefined &&
    (typeof record.portfolioName !== 'string' ||
      record.portfolioName.trim().length === 0)
  ) {
    throw new Error('portfolioName must be a non-empty string.');
  }
  const parsed: PreviewOptions = {};
  if (record.actionsCount !== undefined) {
    if (typeof record.actionsCount !== 'number') {
      throw new Error('actionsCount must be a number.');
    }
    parsed.actionsCount = record.actionsCount;
  }
  if (record.stepsPerAction !== undefined) {
    if (typeof record.stepsPerAction !== 'number') {
      throw new Error('stepsPerAction must be a number.');
    }
    parsed.stepsPerAction = record.stepsPerAction;
  }
  if (record.ensureDelayedStep !== undefined) {
    if (typeof record.ensureDelayedStep !== 'boolean') {
      throw new Error('ensureDelayedStep must be a boolean.');
    }
    parsed.ensureDelayedStep = record.ensureDelayedStep;
  }
  if (record.ensureNextYearStep !== undefined) {
    if (typeof record.ensureNextYearStep !== 'boolean') {
      throw new Error('ensureNextYearStep must be a boolean.');
    }
    parsed.ensureNextYearStep = record.ensureNextYearStep;
  }
  if (record.portfolioName !== undefined) {
    if (typeof record.portfolioName !== 'string') {
      throw new Error('portfolioName must be a string.');
    }
    parsed.portfolioName = record.portfolioName;
  }
  return parsed;
}

async function resolveClarifications(
  questions: ReturnType<typeof parseInstruction>['clarifications'],
  options: PreviewOptions,
  template: Template,
): Promise<void> {
  if (questions.length === 0) {
    return;
  }
  if (!stdin.isTTY || !stdout.isTTY) {
    throw new Error(
      `Instruction is ambiguous: ${questions
        .map((question) => {
          if (question.kind === 'actionsPerUnit') {
            return 'Clarify whether the action count is per unit or for the whole portfolio.';
          }
          return question.kind === 'actionsCount'
            ? 'Specify the number of actions.'
            : 'Specify the number of stages per action.';
        })
        .join(' ')} Run interactively or include the missing details.`,
    );
  }
  const readline = createInterface({ input: stdin, output: stdout });
  try {
    for (const clarification of questions) {
      if (clarification.kind === 'actionsPerUnit') {
        const perUnitCount =
          clarification.count ??
          Number(
            (
              await readline.question(
                'Quantas ações devem ser criadas por unidade? ',
              )
            ).trim(),
          );
        if (!Number.isSafeInteger(perUnitCount) || perUnitCount < 1) {
          throw new Error(
            'The per-unit action count must be a positive integer.',
          );
        }
        const scope = (
          await readline.question(
            'Esse número é por unidade ou o total do portfólio? Responda "unidade" ou "total": ',
          )
        )
          .trim()
          .toLocaleLowerCase('pt-BR');
        if (scope === 'unidade' || scope === 'por unidade') {
          options.actionsCount =
            perUnitCount * Object.keys(template.rules.actionsByUnit).length;
        } else if (scope === 'total') {
          options.actionsCount = perUnitCount;
        } else {
          throw new Error(
            'Please answer "unidade" or "total" to clarify the action count.',
          );
        }
        continue;
      }
      const question =
        clarification.kind === 'actionsCount'
          ? 'Quantas ações devem ser criadas?'
          : 'Quantas etapas devem ser criadas por ação?';
      const count = Number((await readline.question(`${question} `)).trim());
      if (!Number.isSafeInteger(count) || count < 1) {
        throw new Error('The clarified count must be a positive integer.');
      }
      if (clarification.kind === 'actionsCount') {
        options.actionsCount = count;
      } else {
        options.stepsPerAction = count;
      }
    }
  } finally {
    readline.close();
  }
}

async function loadTransformOverrides(
  source: string | undefined,
  template: Template,
): Promise<boolean> {
  if (!source) {
    return false;
  }
  const json = source.trimStart().startsWith('{')
    ? source
    : await readFile(path.resolve(source), 'utf8');
  const normalizedJson = json.replace(/^\uFEFF/, '');
  let value: unknown;
  try {
    value = JSON.parse(normalizedJson);
  } catch (error) {
    throw new Error(
      '--transforms must be valid JSON or a path to a JSON file.',
      {
        cause: error,
      },
    );
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(
      '--transforms must contain a JSON object keyed by "TABLE.COLUMN".',
    );
  }
  const overrides = value as Record<string, unknown>;
  const fieldMapByTarget = new Map(
    template.fieldMaps.map((fieldMap) => [
      `${fieldMap.targetTable}.${fieldMap.targetColumn}`,
      fieldMap,
    ]),
  );
  for (const [target, transform] of Object.entries(overrides)) {
    const fieldMap = fieldMapByTarget.get(target);
    if (!fieldMap) {
      throw new Error(`No field map exists for transform target ${target}.`);
    }
    if (!isTransformSpec(transform)) {
      throw new Error(`Invalid transform definition for ${target}.`);
    }
    fieldMap.transform = transform;
  }
  return true;
}

async function confirmApply(
  template: Template,
  portfolioName = template.name,
): Promise<void> {
  if (!stdin.isTTY || !stdout.isTTY) {
    throw new Error('--apply requires an interactive terminal confirmation.');
  }
  const expected = `APLICAR ${portfolioName}`;
  const readline = createInterface({ input: stdin, output: stdout });
  try {
    const answer = await readline.question(
      `This will insert rows into the configured database. Type "${expected}" to continue: `,
    );
    if (answer !== expected) {
      throw new Error('SQL execution cancelled: confirmation did not match.');
    }
  } finally {
    readline.close();
  }
}

async function writeOutput(filePath: string, content: string): Promise<string> {
  const resolvedPath = path.resolve(filePath);
  await mkdir(path.dirname(resolvedPath), { recursive: true });
  await writeFile(resolvedPath, content, 'utf8');
  return resolvedPath;
}

export async function runCli(args: string[]): Promise<void> {
  const options = parseArguments(args);
  if (!options) {
    printHelp();
    return;
  }

  let template: Template;
  let templateId: string | undefined;
  if (options.templateId) {
    templateId = options.templateId;
    template = await getTemplate(templateId);
  } else {
    if (!options.csvPath || !options.author) {
      throw new Error('CSV path and template author are required.');
    }
    const csvPath = path.resolve(options.csvPath);
    const content = await readFile(csvPath, 'utf8');
    const datasets = parseCsvContent(content);
    template = inferTemplate(datasets, options.author);
  }

  const params = await loadParameters(options.params);
  if (options.instruction) {
    const parsedInstruction = parseInstruction(options.instruction);
    await resolveClarifications(
      parsedInstruction.clarifications,
      parsedInstruction.options,
      template,
    );
    for (const [key, value] of Object.entries(parsedInstruction.options)) {
      const parameterKey = key as keyof PreviewOptions;
      const existing = params[parameterKey];
      if (existing !== undefined && existing !== value) {
        throw new Error(
          `Conflicting values for ${key}; use either --instr or --params for that value.`,
        );
      }
      Object.assign(params, { [key]: value });
    }
    console.log('Instruction understood; generating a preview and SQL script.');
  }
  const transformsChanged = await loadTransformOverrides(
    options.transforms,
    template,
  );
  if (transformsChanged) {
    validateTemplate(template);
  }

  const shouldBuildPreview =
    Boolean(options.previewPath) || options.generate || options.apply;
  const preview = shouldBuildPreview
    ? buildPreview(template, params)
    : undefined;

  if (
    !options.templateId &&
    (options.saveTemplate || (!options.previewPath && !options.instruction))
  ) {
    const csvPath = options.csvPath;
    if (!csvPath) {
      throw new Error('CSV path is required to save a new template.');
    }
    templateId = await saveTemplate(template, path.basename(csvPath));
    console.log(`Template "${template.name}" saved with ID ${templateId}.`);
  }

  if (transformsChanged && options.templateId && templateId) {
    const version = await saveFieldMapVersion(templateId, template.fieldMaps);
    console.log(
      `Field map version ${version} saved for template ${templateId}.`,
    );
  } else if (transformsChanged && !templateId) {
    console.log(
      'Transform overrides applied to this preview; they were not persisted.',
    );
  }

  if (preview && options.previewPath) {
    const previewPath =
      options.previewPath === 'true'
        ? path.join(
            'output',
            `preview_template_${templateId ?? 'preview'}.json`,
          )
        : options.previewPath;
    console.log(
      `Preview generated at: ${await writeOutput(
        previewPath,
        `${JSON.stringify(preview, null, 2)}\n`,
      )}`,
    );
  }

  if (options.generate || options.apply) {
    if (!preview) {
      throw new Error('Unable to build preview for SQL generation.');
    }
    const script = buildApplyScript(template, preview.datasets);
    const outputPath =
      options.outputPath ??
      path.join('output', `apply_template_${templateId ?? 'preview'}.sql`);
    console.log(
      `SQL script generated at: ${await writeOutput(outputPath, script)}`,
    );
    if (options.apply) {
      await confirmApply(
        template,
        preview.parameters.portfolioName ?? undefined,
      );
      await executeApplyScript(script);
      console.log('Generated SQL executed successfully.');
    }
  }
}

export async function closeCliPool(): Promise<void> {
  await closePool();
}
