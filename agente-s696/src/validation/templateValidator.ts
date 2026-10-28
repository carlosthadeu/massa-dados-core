import Ajv from 'ajv';
import { Template, TransformSpec } from '../types.js';

const transformSchema = {
  oneOf: [
    {
      type: 'object',
      properties: {
        type: {
          type: 'string',
          enum: ['integer', 'number', 'timestamp', 'string'],
        },
      },
      required: ['type'],
      additionalProperties: false,
    },
    {
      type: 'object',
      properties: {
        type: { type: 'string', const: 'date' },
        format: { type: 'string', const: 'YYYY-MM-DD' },
      },
      required: ['type', 'format'],
      additionalProperties: false,
    },
    {
      type: 'object',
      properties: {
        type: { type: 'string', const: 'default' },
        value: {
          anyOf: [
            { type: 'string' },
            { type: 'number' },
            { type: 'boolean' },
            { type: 'null' },
          ],
        },
      },
      required: ['type', 'value'],
      additionalProperties: false,
    },
    {
      type: 'object',
      properties: {
        type: { type: 'string', const: 'valueMap' },
        values: { type: 'object', additionalProperties: { type: 'string' } },
        fallback: { type: 'string', nullable: true },
      },
      required: ['type', 'values'],
      additionalProperties: false,
    },
  ],
} as const;

const templateSchema = {
  type: 'object',
  properties: {
    name: { type: 'string', minLength: 1 },
    description: { type: 'string', minLength: 1 },
    author: { type: 'string', minLength: 1 },
    fieldMaps: {
      type: 'array',
      minItems: 1,
      items: {
        type: 'object',
        properties: {
          source: { type: 'string', minLength: 1 },
          targetTable: { type: 'string', minLength: 1 },
          targetColumn: { type: 'string', minLength: 1 },
          transform: transformSchema,
        },
        required: ['source', 'targetTable', 'targetColumn', 'transform'],
        additionalProperties: false,
      },
    },
    rules: {
      type: 'object',
      properties: {
        sourcePortfolioId: { type: 'string', minLength: 1 },
        actionCount: { type: 'integer', minimum: 1 },
        actionsByUnit: {
          type: 'object',
          additionalProperties: { type: 'integer', minimum: 1 },
        },
        actionStatus: {
          type: 'object',
          properties: {
            column: { type: 'string', minLength: 1 },
            value: { type: 'string', minLength: 1 },
            label: { type: 'string', minLength: 1 },
          },
          required: ['column', 'value', 'label'],
          additionalProperties: false,
        },
        stagesPerAction: {
          type: 'object',
          properties: {
            minimum: { type: 'integer', minimum: 1 },
            maximum: { type: 'integer', minimum: 1 },
          },
          required: ['minimum', 'maximum'],
          additionalProperties: false,
        },
      },
      required: [
        'sourcePortfolioId',
        'actionCount',
        'actionsByUnit',
        'actionStatus',
        'stagesPerAction',
      ],
      additionalProperties: false,
    },
    datasets: {
      type: 'array',
      minItems: 1,
      items: {
        type: 'object',
        properties: {
          key: { type: 'string', minLength: 1 },
          targetTable: { type: 'string', minLength: 1 },
          primaryKey: { type: 'string', minLength: 1 },
          records: {
            type: 'array',
            minItems: 1,
            items: {
              type: 'object',
              additionalProperties: {
                anyOf: [{ type: 'string' }, { type: 'null' }],
              },
            },
          },
          foreignKeys: {
            type: 'array',
            items: {
              type: 'object',
              properties: {
                column: { type: 'string', minLength: 1 },
                parentDataset: { type: 'string', minLength: 1 },
              },
              required: ['column', 'parentDataset'],
              additionalProperties: false,
            },
          },
        },
        required: [
          'key',
          'targetTable',
          'primaryKey',
          'records',
          'foreignKeys',
        ],
        additionalProperties: false,
      },
    },
  },
  required: ['name', 'description', 'author', 'fieldMaps', 'rules', 'datasets'],
  additionalProperties: false,
} as const;

const validate = new Ajv({ allErrors: true }).compile<Template>(templateSchema);

export function isTransformSpec(value: unknown): value is TransformSpec {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return false;
  }
  const transform = value as Record<string, unknown>;
  switch (transform.type) {
    case 'integer':
    case 'number':
    case 'timestamp':
    case 'string':
      return Object.keys(transform).length === 1;
    case 'date':
      return (
        transform.format === 'YYYY-MM-DD' && Object.keys(transform).length === 2
      );
    case 'default':
      return (
        Object.hasOwn(transform, 'value') && Object.keys(transform).length === 2
      );
    case 'valueMap':
      return (
        !!transform.values &&
        typeof transform.values === 'object' &&
        !Array.isArray(transform.values) &&
        Object.values(transform.values).every(
          (item) => typeof item === 'string',
        ) &&
        (transform.fallback === undefined ||
          typeof transform.fallback === 'string') &&
        Object.keys(transform).every((key) =>
          ['type', 'values', 'fallback'].includes(key),
        )
      );
    default:
      return false;
  }
}

export function validateTemplate(template: Template): void {
  if (!validate(template)) {
    const errors =
      validate.errors?.map(
        (error) => `${error.instancePath || '/'} ${error.message}`,
      ) ?? [];
    throw new Error(`Invalid template: ${errors.join('; ')}`);
  }
  if (
    template.rules.stagesPerAction.minimum >
    template.rules.stagesPerAction.maximum
  ) {
    throw new Error(
      'Invalid template: minimum stages per action exceeds maximum.',
    );
  }

  const targets = new Set(
    template.datasets.flatMap((dataset) =>
      Object.keys(dataset.records[0] ?? {}).map(
        (column) => `${dataset.targetTable}.${column}`,
      ),
    ),
  );
  for (const fieldMap of template.fieldMaps) {
    if (!targets.has(`${fieldMap.targetTable}.${fieldMap.targetColumn}`)) {
      throw new Error(
        `Invalid template: field map target ${fieldMap.targetTable}.${fieldMap.targetColumn} is absent from its example dataset.`,
      );
    }
  }
}
