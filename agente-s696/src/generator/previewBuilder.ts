import {
  CsvDataset,
  CsvRecord,
  Preview,
  PreviewOptions,
  Template,
} from '../types.js';
import { transformRecord } from './transforms.js';
import { validateTemplate } from '../validation/templateValidator.js';

interface GeneratedRecord {
  source: CsvRecord;
  simulatedId: number;
}

function requireDataset(template: Template, key: string): CsvDataset {
  const dataset = template.datasets.find((candidate) => candidate.key === key);
  if (!dataset || dataset.records.length === 0) {
    throw new Error(`Template is missing records for ${key}.`);
  }
  return dataset;
}

function requireInteger(value: number, label: string, maximum: number): void {
  if (!Number.isSafeInteger(value) || value < 1 || value > maximum) {
    throw new Error(`${label} must be an integer between 1 and ${maximum}.`);
  }
}

function cloneForInsert(
  dataset: CsvDataset,
  source: CsvRecord,
  id: number,
  fieldMaps: Template['fieldMaps'],
): CsvRecord {
  const record = transformRecord(dataset, source, fieldMaps);
  record[dataset.primaryKey] = String(id);
  return record;
}

function simulatedForeignKey(
  record: CsvRecord,
  column: string,
  id: number,
): CsvRecord {
  return { ...record, [column]: String(id) };
}

function copyName(value: string | null, copyNumber: number): string | null {
  if (!value) {
    return value;
  }
  return `${value} - Cópia ${copyNumber}`;
}

export function buildPreview(
  template: Template,
  options: PreviewOptions = {},
): Preview {
  validateTemplate(template);
  const portfolioDataset = requireDataset(template, 'portfolio');
  const actionsDataset = requireDataset(template, 'actions');
  const stagesDataset = requireDataset(template, 'stages');
  const linksDataset = requireDataset(template, 'actionLinks');
  const domainsDataset = requireDataset(template, 'actionDomains');
  const deliveriesDataset = requireDataset(template, 'deliveries');

  const actionsCount = options.actionsCount ?? template.rules.actionCount;
  requireInteger(actionsCount, 'actionsCount', 1000);
  if (options.stepsPerAction !== undefined) {
    requireInteger(options.stepsPerAction, 'stepsPerAction', 100);
  }

  const portfolioSource = portfolioDataset.records[0];
  if (!portfolioSource) {
    throw new Error('Template has no portfolio source record.');
  }
  const portfolioRecord = cloneForInsert(
    portfolioDataset,
    portfolioSource,
    1,
    template.fieldMaps,
  );
  if (options.portfolioName) {
    portfolioRecord.NM_PTF = options.portfolioName;
  }
  const portfolioId = 1;
  const generated: Record<string, CsvRecord[]> = {
    portfolio: [portfolioRecord],
    actions: [],
    stages: [],
    actionLinks: [],
    actionDomains: [],
    deliveries: [],
  };
  const sourceActions = actionsDataset.records;
  const sourceStagesByAction = new Map<string, CsvRecord[]>();
  for (const stage of stagesDataset.records) {
    const actionId = stage.SQ_ACO_ETT;
    if (actionId) {
      const stages = sourceStagesByAction.get(actionId) ?? [];
      stages.push(stage);
      sourceStagesByAction.set(actionId, stages);
    }
  }
  const sourceDeliveriesByStage = new Map<string, CsvRecord[]>();
  for (const delivery of deliveriesDataset.records) {
    const stageId = delivery.SQ_ETP_ACO;
    if (stageId) {
      const deliveries = sourceDeliveriesByStage.get(stageId) ?? [];
      deliveries.push(delivery);
      sourceDeliveriesByStage.set(stageId, deliveries);
    }
  }

  const actionCopies: Array<{ source: CsvRecord; id: number }> = [];
  for (let index = 0; index < actionsCount; index += 1) {
    const source = sourceActions[index % sourceActions.length];
    if (!source) {
      throw new Error('Template has no source action record.');
    }
    const id = index + 1;
    let record = cloneForInsert(actionsDataset, source, id, template.fieldMaps);
    if (index >= sourceActions.length) {
      record.NM_ACO_ETT = copyName(record.NM_ACO_ETT, id);
    }
    record = simulatedForeignKey(record, 'SQ_PTF_ACO', portfolioId);
    generated.actions.push(record);
    actionCopies.push({ source, id });
  }

  let nextStageId = 1;
  for (const action of actionCopies) {
    const sourceActionId = action.source[actionsDataset.primaryKey];
    if (!sourceActionId) {
      throw new Error('An action record is missing its source ID.');
    }
    const sourceStages = sourceStagesByAction.get(sourceActionId) ?? [];
    if (sourceStages.length === 0) {
      throw new Error(`Action ${sourceActionId} has no source stages.`);
    }
    const count = options.stepsPerAction ?? sourceStages.length;
    const selectedStages = Array.from({ length: count }, (_, index) => ({
      source: sourceStages[index % sourceStages.length],
      copyNumber: index >= sourceStages.length ? index + 1 : undefined,
    }));
    if (options.ensureNextYearStep || options.ensureDelayedStep) {
      const lastStage = selectedStages[selectedStages.length - 1]?.source;
      if (!lastStage) {
        throw new Error('Cannot add an extra stage without a base stage.');
      }
      const year = Number(lastStage.AA_REA);
      if (!Number.isSafeInteger(year)) {
        throw new Error(
          `Invalid realization year in source stage "${lastStage.AA_REA}".`,
        );
      }
      if (options.ensureNextYearStep) {
        const nextYear = { ...lastStage };
        nextYear.AA_REA = String(year + 1);
        nextYear.DE_ETP_ACO = `${lastStage.DE_ETP_ACO ?? 'Etapa'} - Próximo ano`;
        selectedStages.push({ source: nextYear, copyNumber: undefined });
      }
      if (options.ensureDelayedStep) {
        const delayed = { ...lastStage };
        delayed.AA_REA = String(year - 1);
        delayed.DE_ETP_ACO = `${lastStage.DE_ETP_ACO ?? 'Etapa'} - Etapa atrasada`;
        selectedStages.push({ source: delayed, copyNumber: undefined });
      }
    }

    const generatedStagesForAction: GeneratedRecord[] = [];
    for (const { source: sourceStage, copyNumber } of selectedStages) {
      const id = nextStageId++;
      const stageForInsert = copyNumber
        ? {
            ...sourceStage,
            DE_ETP_ACO: copyName(sourceStage.DE_ETP_ACO, copyNumber),
          }
        : sourceStage;
      let record = cloneForInsert(
        stagesDataset,
        stageForInsert,
        id,
        template.fieldMaps,
      );
      record = simulatedForeignKey(record, 'SQ_ACO_ETT', action.id);
      generated.stages.push(record);
      generatedStagesForAction.push({
        source: sourceStage,
        simulatedId: id,
      });
    }
    for (const source of linksDataset.records.filter(
      (record) => record.SQ_ACO_ETT === sourceActionId,
    )) {
      const record = cloneForInsert(
        linksDataset,
        source,
        generated.actionLinks.length + 1,
        template.fieldMaps,
      );
      generated.actionLinks.push(
        simulatedForeignKey(record, 'SQ_ACO_ETT', action.id),
      );
    }
    for (const source of domainsDataset.records.filter(
      (record) => record.SQ_ACO_ETT === sourceActionId,
    )) {
      const record = cloneForInsert(
        domainsDataset,
        source,
        generated.actionDomains.length + 1,
        template.fieldMaps,
      );
      generated.actionDomains.push(
        simulatedForeignKey(record, 'SQ_ACO_ETT', action.id),
      );
    }
    for (const stage of generatedStagesForAction) {
      const sourceStageId = stage.source[stagesDataset.primaryKey];
      if (!sourceStageId) {
        throw new Error('A stage record is missing its source ID.');
      }
      for (const source of sourceDeliveriesByStage.get(sourceStageId) ?? []) {
        const record = cloneForInsert(
          deliveriesDataset,
          source,
          generated.deliveries.length + 1,
          template.fieldMaps,
        );
        generated.deliveries.push(
          simulatedForeignKey(record, 'SQ_ETP_ACO', stage.simulatedId),
        );
      }
    }
  }

  const datasets = template.datasets.map((dataset) => ({
    ...dataset,
    records: generated[dataset.key] ?? [],
  }));
  return {
    templateName: template.name,
    parameters: {
      actionsCount,
      stepsPerAction: options.stepsPerAction ?? null,
      ensureNextYearStep: options.ensureNextYearStep ?? false,
      ensureDelayedStep: options.ensureDelayedStep ?? false,
      portfolioName: options.portfolioName ?? null,
    },
    simulatedIds: true,
    datasets,
  };
}
