import { CsvDataset, CsvRecord, FieldMap, Template } from '../types.js';

function inferTransform(values: CsvRecord[string][]): FieldMap['transform'] {
  const samples = values.filter((value): value is string => value !== null);
  if (samples.length === 0) {
    return { type: 'string' };
  }
  if (samples.every((value) => /^-?\d+$/.test(value))) {
    return { type: 'integer' };
  }
  if (samples.every((value) => /^-?\d+(?:[.,]\d+)?$/.test(value))) {
    return { type: 'number' };
  }
  if (samples.every((value) => /^\d{4}-\d{2}-\d{2}(?:[ T].*)?$/.test(value))) {
    return { type: 'timestamp' };
  }
  return { type: 'string' };
}

function requireDataset(datasets: CsvDataset[], key: string): CsvDataset {
  const dataset = datasets.find((candidate) => candidate.key === key);
  if (!dataset || dataset.records.length === 0) {
    throw new Error(`CSV example must contain at least one ${key} record.`);
  }
  return dataset;
}

function requireValue(record: CsvRecord, column: string): string {
  const value = record[column];
  if (!value) {
    throw new Error(
      `Required value ${column} is missing from the portfolio CSV.`,
    );
  }
  return value;
}

export function inferTemplate(
  datasets: CsvDataset[],
  author: string,
): Template {
  if (!author.trim()) {
    throw new Error('The template author must not be empty.');
  }

  for (const key of [
    'portfolio',
    'actions',
    'stages',
    'actionLinks',
    'actionDomains',
    'deliveries',
  ]) {
    requireDataset(datasets, key);
  }

  const portfolio = requireDataset(datasets, 'portfolio');
  const actions = requireDataset(datasets, 'actions');
  const stages = requireDataset(datasets, 'stages');
  const portfolioRow = portfolio.records[0];
  const sourcePortfolioId = requireValue(portfolioRow, portfolio.primaryKey);
  const portfolioName = requireValue(portfolioRow, 'NM_PTF');
  const actionIds = new Set(
    actions.records.map((action) => requireValue(action, actions.primaryKey)),
  );
  const stageIds = new Set(
    stages.records.map((stage) => requireValue(stage, stages.primaryKey)),
  );

  for (const action of actions.records) {
    if (requireValue(action, 'SQ_PTF_ACO') !== sourcePortfolioId) {
      throw new Error(
        'All actions in the example must reference its portfolio record.',
      );
    }
  }
  for (const stage of stages.records) {
    if (!actionIds.has(requireValue(stage, 'SQ_ACO_ETT'))) {
      throw new Error(
        'A stage references an action that is not present in the example.',
      );
    }
  }
  for (const dataset of datasets.filter(
    (candidate) =>
      candidate.key === 'actionLinks' || candidate.key === 'actionDomains',
  )) {
    for (const record of dataset.records) {
      if (!actionIds.has(requireValue(record, 'SQ_ACO_ETT'))) {
        throw new Error(
          `${dataset.targetTable} references an action that is not present in the example.`,
        );
      }
    }
  }
  const deliveries = requireDataset(datasets, 'deliveries');
  for (const delivery of deliveries.records) {
    if (!stageIds.has(requireValue(delivery, 'SQ_ETP_ACO'))) {
      throw new Error(
        'A delivery references a stage that is not present in the example.',
      );
    }
  }

  const actionsByUnit: Record<string, number> = {};
  for (const action of actions.records) {
    const unit = requireValue(action, 'CD_UND');
    actionsByUnit[unit] = (actionsByUnit[unit] ?? 0) + 1;
  }

  const statuses = new Set(
    actions.records.map((action) => requireValue(action, 'ST_ACO_ETT')),
  );
  if (statuses.size !== 1) {
    throw new Error(
      'The example must use the same action status for every action.',
    );
  }
  const stageCounts = new Map<string, number>();
  for (const stage of stages.records) {
    const actionId = requireValue(stage, 'SQ_ACO_ETT');
    stageCounts.set(actionId, (stageCounts.get(actionId) ?? 0) + 1);
  }
  if (stageCounts.size !== actions.records.length) {
    throw new Error(
      'Every action in the example must have at least one stage.',
    );
  }

  const fieldMaps = datasets.flatMap((dataset) =>
    Object.keys(dataset.records[0]).map((column) => ({
      source: column,
      targetTable: dataset.targetTable,
      targetColumn: column,
      transform: inferTransform(
        dataset.records.map((record) => record[column]),
      ),
    })),
  );
  const counts = [...stageCounts.values()];

  return {
    name: portfolioName,
    description: `Template inferred from portfolio ${portfolioName}, including ${actions.records.length} strategic actions.`,
    author: author.trim(),
    fieldMaps,
    rules: {
      sourcePortfolioId,
      actionCount: actions.records.length,
      actionsByUnit,
      actionStatus: {
        column: 'ST_ACO_ETT',
        value: [...statuses][0],
        label:
          [...statuses][0] === '10'
            ? 'Análise Diretoria'
            : 'Status inferido do CSV',
      },
      stagesPerAction: {
        minimum: Math.min(...counts),
        maximum: Math.max(...counts),
      },
    },
    datasets,
  };
}
