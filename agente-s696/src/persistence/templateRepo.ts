import { getPool } from './db.js';
import { loadConfig } from '../config.js';
import { FieldMap, Template, TransformSpec } from '../types.js';
import {
  isTransformSpec,
  validateTemplate,
} from '../validation/templateValidator.js';

function quoteIdentifier(identifier: string): string {
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(identifier)) {
    throw new Error(`Invalid PostgreSQL identifier: ${identifier}`);
  }
  return `"${identifier}"`;
}

export async function saveTemplate(
  template: Template,
  sourceFile: string,
): Promise<string> {
  validateTemplate(template);
  const schema = quoteIdentifier(loadConfig().databaseSchema);
  const pool = getPool();
  const client = await pool.connect();

  try {
    await client.query('BEGIN');
    const templateResult = await client.query<{ template_id: string }>(
      `INSERT INTO ${schema}.model_template (name, description, created_by, definition)
       VALUES ($1, $2, $3, $4::jsonb)
       RETURNING template_id`,
      [
        template.name,
        template.description,
        template.author,
        JSON.stringify({
          fieldMaps: template.fieldMaps,
          rules: template.rules,
        }),
      ],
    );
    const templateId = templateResult.rows[0]?.template_id;
    if (!templateId) {
      throw new Error('The database did not return the saved template ID.');
    }

    await client.query(
      `INSERT INTO ${schema}.model_field_map_version (template_id, version, field_maps)
       VALUES ($1, 1, $2::jsonb)`,
      [templateId, JSON.stringify(template.fieldMaps)],
    );

    for (const dataset of template.datasets) {
      await client.query(
        `INSERT INTO ${schema}.model_example (template_id, source_file, dataset_key, records)
         VALUES ($1, $2, $3, $4::jsonb)`,
        [templateId, sourceFile, dataset.key, JSON.stringify(dataset.records)],
      );
    }
    await client.query('COMMIT');
    return templateId;
  } catch (error) {
    try {
      await client.query('ROLLBACK');
    } catch (rollbackError) {
      throw new AggregateError(
        [error, rollbackError],
        'Saving the template failed and the transaction rollback also failed.',
      );
    }
    throw error;
  } finally {
    client.release();
  }
}

const datasetMetadata: Record<
  string,
  Pick<
    Template['datasets'][number],
    'targetTable' | 'primaryKey' | 'foreignKeys'
  >
> = {
  portfolio: {
    targetTable: 'T696POAC',
    primaryKey: 'SQ_PTF_ACO',
    foreignKeys: [],
  },
  actions: {
    targetTable: 'T696ACES',
    primaryKey: 'SQ_ACO_ETT',
    foreignKeys: [{ column: 'SQ_PTF_ACO', parentDataset: 'portfolio' }],
  },
  stages: {
    targetTable: 'T696ETAC',
    primaryKey: 'SQ_ETP_ACO',
    foreignKeys: [{ column: 'SQ_ACO_ETT', parentDataset: 'actions' }],
  },
  actionLinks: {
    targetTable: 'T696VIAC',
    primaryKey: 'SQ_VNC_ACO',
    foreignKeys: [{ column: 'SQ_ACO_ETT', parentDataset: 'actions' }],
  },
  actionDomains: {
    targetTable: 'T696DOAC',
    primaryKey: 'SQ_ID_DMN',
    foreignKeys: [{ column: 'SQ_ACO_ETT', parentDataset: 'actions' }],
  },
  deliveries: {
    targetTable: 'T696ENAC',
    primaryKey: 'SQ_ENT_ACO',
    foreignKeys: [{ column: 'SQ_ETP_ACO', parentDataset: 'stages' }],
  },
};

export async function getTemplate(templateId: string): Promise<Template> {
  if (!/^[1-9]\d*$/.test(templateId)) {
    throw new Error('templateId must be a positive numeric ID.');
  }
  const schema = quoteIdentifier(loadConfig().databaseSchema);
  const client = await getPool().connect();
  try {
    const result = await client.query<{
      name: string;
      description: string;
      created_by: string;
      definition: {
        fieldMaps: Template['fieldMaps'];
        rules: Template['rules'];
      };
    }>(
      `SELECT name, description, created_by, definition
       FROM ${schema}.model_template WHERE template_id = $1`,
      [templateId],
    );
    const row = result.rows[0];
    if (!row) {
      throw new Error(`Template ${templateId} was not found.`);
    }

    const examples = await client.query<{
      dataset_key: string;
      records: Template['datasets'][number]['records'];
    }>(
      `SELECT dataset_key, records FROM ${schema}.model_example
       WHERE template_id = $1 ORDER BY example_id`,
      [templateId],
    );
    const datasets = examples.rows.map((example) => {
      const metadata = datasetMetadata[example.dataset_key];
      if (!metadata) {
        throw new Error(
          `Template contains unsupported dataset ${example.dataset_key}.`,
        );
      }
      return {
        key: example.dataset_key,
        ...metadata,
        records: example.records,
      };
    });
    const fieldMapResult = await client.query<{ field_maps: unknown }>(
      `SELECT field_maps FROM ${schema}.model_field_map_version
       WHERE template_id = $1 ORDER BY version DESC LIMIT 1`,
      [templateId],
    );
    const template: Template = {
      name: row.name,
      description: row.description,
      author: row.created_by,
      ...row.definition,
      fieldMaps: normalizeFieldMaps(
        fieldMapResult.rows[0]?.field_maps ?? row.definition.fieldMaps,
      ),
      datasets,
    };
    validateTemplate(template);
    return template;
  } finally {
    client.release();
  }
}

function normalizeFieldMaps(value: unknown): Template['fieldMaps'] {
  if (!Array.isArray(value)) {
    throw new Error('Stored template field maps must be a JSON array.');
  }
  return value.map((fieldMap) => {
    if (!fieldMap || typeof fieldMap !== 'object' || Array.isArray(fieldMap)) {
      throw new Error('Stored template contains an invalid field map.');
    }
    const candidate = fieldMap as Record<string, unknown>;
    const transform = candidate.transform;
    if (
      typeof candidate.source !== 'string' ||
      typeof candidate.targetTable !== 'string' ||
      typeof candidate.targetColumn !== 'string'
    ) {
      throw new Error('Stored template contains an incomplete field map.');
    }
    const legacyTransform = normalizeLegacyTransform(transform);
    if (legacyTransform) {
      return {
        source: candidate.source,
        targetTable: candidate.targetTable,
        targetColumn: candidate.targetColumn,
        transform: legacyTransform,
      } satisfies FieldMap;
    }
    if (!isTransformSpec(transform)) {
      throw new Error(
        'Stored template contains an invalid field map transform.',
      );
    }
    return {
      source: candidate.source,
      targetTable: candidate.targetTable,
      targetColumn: candidate.targetColumn,
      transform,
    };
  });
}

function normalizeLegacyTransform(value: unknown): TransformSpec | undefined {
  switch (value) {
    case 'integer':
      return { type: 'integer' };
    case 'number':
      return { type: 'number' };
    case 'timestamp':
      return { type: 'timestamp' };
    case 'string':
      return { type: 'string' };
    default:
      return undefined;
  }
}

export async function saveFieldMapVersion(
  templateId: string,
  fieldMaps: Template['fieldMaps'],
): Promise<number> {
  const schema = quoteIdentifier(loadConfig().databaseSchema);
  const result = await getPool().query<{ version: number }>(
    `INSERT INTO ${schema}.model_field_map_version (template_id, version, field_maps)
     VALUES (
       $1,
       COALESCE((SELECT MAX(version) + 1 FROM ${schema}.model_field_map_version WHERE template_id = $1), 1),
       $2::jsonb
     )
     RETURNING version`,
    [templateId, JSON.stringify(fieldMaps)],
  );
  const version = result.rows[0]?.version;
  if (version === undefined) {
    throw new Error(
      `Unable to save field map version for template ${templateId}.`,
    );
  }
  return version;
}

export async function executeApplyScript(script: string): Promise<void> {
  if (
    !script.startsWith(
      '-- Review the target database and search_path before applying this script.\nBEGIN;\nDO $portfolio$',
    )
  ) {
    throw new Error(
      'Refusing to execute SQL that was not generated by the portfolio builder.',
    );
  }
  const client = await getPool().connect();
  try {
    await client.query(script);
  } catch (error) {
    try {
      await client.query('ROLLBACK');
    } catch (rollbackError) {
      throw new AggregateError(
        [error, rollbackError],
        'Applying the generated SQL failed and transaction rollback also failed.',
      );
    }
    throw error;
  } finally {
    client.release();
  }
}
