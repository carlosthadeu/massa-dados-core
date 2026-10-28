export type CsvValue = string | null;
export type CsvRecord = Record<string, CsvValue>;

export interface CsvDataset {
  key: string;
  targetTable: string;
  primaryKey: string;
  records: CsvRecord[];
  foreignKeys: Array<{ column: string; parentDataset: string }>;
}

export interface FieldMap {
  source: string;
  targetTable: string;
  targetColumn: string;
  transform: TransformSpec;
}

export type TransformSpec =
  | { type: 'integer' | 'number' | 'timestamp' | 'string' }
  | { type: 'date'; format: 'YYYY-MM-DD' }
  | { type: 'default'; value: string | number | boolean | null }
  | { type: 'valueMap'; values: Record<string, string>; fallback?: string };

export interface PreviewOptions {
  actionsCount?: number;
  stepsPerAction?: number;
  ensureNextYearStep?: boolean;
  ensureDelayedStep?: boolean;
  portfolioName?: string;
}

export interface Preview {
  templateName: string;
  parameters: {
    actionsCount: number;
    stepsPerAction: number | null;
    ensureNextYearStep: boolean;
    ensureDelayedStep: boolean;
    portfolioName: string | null;
  };
  simulatedIds: true;
  datasets: CsvDataset[];
}

export interface Template {
  name: string;
  description: string;
  author: string;
  fieldMaps: FieldMap[];
  rules: {
    sourcePortfolioId: string;
    actionCount: number;
    actionsByUnit: Record<string, number>;
    actionStatus: {
      column: string;
      value: string;
      label: string;
    };
    stagesPerAction: {
      minimum: number;
      maximum: number;
    };
  };
  datasets: CsvDataset[];
}
