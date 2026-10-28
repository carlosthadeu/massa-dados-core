import { CsvDataset, Template } from '../types.js';
import { validateTemplate } from '../validation/templateValidator.js';

const insertOrder = [
  'portfolio',
  'actions',
  'stages',
  'actionLinks',
  'actionDomains',
  'deliveries',
];

function quoteIdentifier(identifier: string): string {
  return `"${identifier.replaceAll('"', '""')}"`;
}

function sqlString(value: string): string {
  return `'${value.replaceAll("'", "''")}'`;
}

function datasetInsert(dataset: CsvDataset): string {
  const insertedColumns = Object.keys(dataset.records[0] ?? {}).filter(
    (column) => column !== dataset.primaryKey,
  );
  if (insertedColumns.length === 0) {
    throw new Error(`${dataset.targetTable} has no insertable columns.`);
  }

  const columns = insertedColumns.map(quoteIdentifier).join(', ');
  const selectedColumns = insertedColumns.map(quoteIdentifier).join(', ');
  const table = quoteIdentifier(dataset.targetTable);
  const primaryKey = quoteIdentifier(dataset.primaryKey);
  const statement = `INSERT INTO ${table} (${columns}) SELECT ${selectedColumns} FROM jsonb_populate_record(NULL::${table}, $1) RETURNING ${primaryKey}`;
  const lines = [
    `    FOR v_record IN SELECT value FROM jsonb_array_elements(${sqlString(JSON.stringify(dataset.records))}::jsonb) LOOP`,
    `      v_old_id := v_record ->> ${sqlString(dataset.primaryKey)};`,
  ];

  for (const foreignKey of dataset.foreignKeys) {
    const mapKey = `format(${sqlString(`${foreignKey.parentDataset}:%s`)}, v_record ->> ${sqlString(foreignKey.column)})`;
    lines.push(
      `      IF NOT (v_id_map ? ${mapKey}) THEN`,
      `        RAISE EXCEPTION ${sqlString(`Missing generated parent ID for ${foreignKey.column}.`)};`,
      '      END IF;',
      `      v_record := v_record || jsonb_build_object(${sqlString(foreignKey.column)}, (v_id_map ->> ${mapKey})::bigint);`,
    );
  }

  lines.push(
    `      v_record := v_record - ${sqlString(dataset.primaryKey)};`,
    `      EXECUTE ${sqlString(statement)} INTO v_generated_id USING v_record;`,
    `      v_id_map := v_id_map || jsonb_build_object(${sqlString(`${dataset.key}:`)} || v_old_id, v_generated_id);`,
    '    END LOOP;',
  );
  return lines.join('\n');
}

export function buildApplyScript(
  template: Template,
  datasetRows: CsvDataset[] = template.datasets,
): string {
  validateTemplate(template);
  const templateByKey = new Map(
    template.datasets.map((dataset) => [dataset.key, dataset]),
  );
  if (datasetRows.length !== template.datasets.length) {
    throw new Error(
      'Generated data must include every dataset from the template.',
    );
  }
  for (const dataset of datasetRows) {
    const source = templateByKey.get(dataset.key);
    if (
      !source ||
      source.targetTable !== dataset.targetTable ||
      source.primaryKey !== dataset.primaryKey ||
      JSON.stringify(source.foreignKeys) !==
        JSON.stringify(dataset.foreignKeys) ||
      source.records.some((record) =>
        Object.keys(record).some((column) =>
          dataset.records.some((row) => !(column in row)),
        ),
      ) ||
      dataset.records.some((record) =>
        Object.keys(record).some(
          (column) => !(column in (source.records[0] ?? {})),
        ),
      )
    ) {
      throw new Error(
        `Generated dataset ${dataset.key} does not match the validated template.`,
      );
    }
  }
  const datasets = insertOrder
    .map((key) => datasetRows.find((dataset) => dataset.key === key))
    .filter(
      (dataset): dataset is CsvDataset =>
        dataset !== undefined && dataset.records.length > 0,
    );
  if (datasets.length === 0) {
    throw new Error(
      'The template contains missing or unsupported dataset sections.',
    );
  }

  return [
    '-- Review the target database and search_path before applying this script.',
    'BEGIN;',
    'DO $portfolio$',
    'DECLARE',
    '  v_record jsonb;',
    '  v_old_id text;',
    '  v_generated_id bigint;',
    "  v_id_map jsonb := '{}'::jsonb;",
    'BEGIN',
    ...datasets.map(datasetInsert),
    'END;',
    '$portfolio$;',
    'COMMIT;',
    '',
  ].join('\n');
}
