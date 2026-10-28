import { parse } from 'csv-parse/sync';
import { CsvDataset, CsvRecord } from '../types.js';

interface DatasetDefinition {
  key: string;
  primaryKey: string;
  targetTable: string;
  requiredColumns: string[];
  foreignKeys: CsvDataset['foreignKeys'];
}

const definitions: DatasetDefinition[] = [
  {
    key: 'portfolio',
    primaryKey: 'SQ_PTF_ACO',
    targetTable: 'T696POAC',
    requiredColumns: ['NM_PTF', 'AA_REA'],
    foreignKeys: [],
  },
  {
    key: 'actions',
    primaryKey: 'SQ_ACO_ETT',
    targetTable: 'T696ACES',
    requiredColumns: ['NM_ACO_ETT', 'CD_UND', 'ST_ACO_ETT', 'SQ_PTF_ACO'],
    foreignKeys: [{ column: 'SQ_PTF_ACO', parentDataset: 'portfolio' }],
  },
  {
    key: 'stages',
    primaryKey: 'SQ_ETP_ACO',
    targetTable: 'T696ETAC',
    requiredColumns: ['SQ_ACO_ETT', 'DE_ETP_ACO', 'AA_REA'],
    foreignKeys: [{ column: 'SQ_ACO_ETT', parentDataset: 'actions' }],
  },
  {
    key: 'actionLinks',
    primaryKey: 'SQ_VNC_ACO',
    targetTable: 'T696VIAC',
    requiredColumns: ['SQ_ACO_ETT', 'SQ_ORT_ETT'],
    foreignKeys: [{ column: 'SQ_ACO_ETT', parentDataset: 'actions' }],
  },
  {
    key: 'actionDomains',
    primaryKey: 'SQ_ID_DMN',
    targetTable: 'T696DOAC',
    requiredColumns: ['SQ_ACO_ETT', 'SQ_DMN'],
    foreignKeys: [{ column: 'SQ_ACO_ETT', parentDataset: 'actions' }],
  },
  {
    key: 'deliveries',
    primaryKey: 'SQ_ENT_ACO',
    targetTable: 'T696ENAC',
    requiredColumns: ['SQ_ETP_ACO', 'MM_ENT', 'PC_PLJ_ENT'],
    foreignKeys: [{ column: 'SQ_ETP_ACO', parentDataset: 'stages' }],
  },
];

function isBlankRecord(record: string[]): boolean {
  return record.every((value) => value.trim() === '');
}

function getDefinition(headers: string[]): DatasetDefinition | undefined {
  return definitions.find(
    (definition) =>
      headers.includes(definition.primaryKey) &&
      definition.requiredColumns.every((column) => headers.includes(column)),
  );
}

export function parseCsvContent(content: string): CsvDataset[] {
  const rows = parse(content, {
    bom: true,
    relax_column_count: true,
    skip_empty_lines: true,
    trim: true,
  }) as string[][];

  const datasets: CsvDataset[] = [];
  const foundSections = new Set<string>();
  let current:
    | { definition: DatasetDefinition; headers: string[]; records: CsvRecord[] }
    | undefined;

  for (const [index, row] of rows.entries()) {
    if (isBlankRecord(row)) {
      continue;
    }

    const headers = row.map((value) => value.trim());
    const definition = getDefinition(headers);
    if (definition) {
      if (foundSections.has(definition.key)) {
        throw new Error(
          `CSV contains more than one ${definition.key} section.`,
        );
      }
      foundSections.add(definition.key);
      if (current) {
        datasets.push({
          key: current.definition.key,
          targetTable: current.definition.targetTable,
          primaryKey: current.definition.primaryKey,
          records: current.records,
          foreignKeys: current.definition.foreignKeys,
        });
      }
      current = { definition, headers, records: [] };
      continue;
    }

    if (!current) {
      continue;
    }
    if (row.length !== current.headers.length) {
      throw new Error(
        `CSV record ${index + 1} has ${row.length} columns; expected ${current.headers.length} for ${current.definition.targetTable}.`,
      );
    }

    const record = Object.fromEntries(
      current.headers.map((header, columnIndex) => {
        const value = row[columnIndex]?.trim() ?? '';
        return [header, value === '' ? null : value];
      }),
    ) as CsvRecord;
    current.records.push(record);
  }

  if (current) {
    datasets.push({
      key: current.definition.key,
      targetTable: current.definition.targetTable,
      primaryKey: current.definition.primaryKey,
      records: current.records,
      foreignKeys: current.definition.foreignKeys,
    });
  }

  if (datasets.length === 0) {
    throw new Error('No supported CSV sections found in the input file.');
  }

  for (const dataset of datasets) {
    const seenIds = new Set<string>();
    for (const [index, record] of dataset.records.entries()) {
      const id = record[dataset.primaryKey];
      if (!id) {
        throw new Error(
          `Missing ${dataset.primaryKey} in ${dataset.targetTable} record ${index + 1}.`,
        );
      }
      if (seenIds.has(id)) {
        throw new Error(
          `Duplicate ${dataset.primaryKey} value "${id}" in ${dataset.targetTable}.`,
        );
      }
      seenIds.add(id);
    }
  }

  return datasets;
}
