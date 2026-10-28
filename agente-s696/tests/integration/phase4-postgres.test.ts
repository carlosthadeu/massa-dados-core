import { readFileSync } from 'node:fs';
import path from 'node:path';
import { buildPreview } from '../../src/generator/previewBuilder.js';
import { buildApplyScript } from '../../src/generator/sqlBuilder.js';
import { parseCsvContent } from '../../src/csv/parser.js';
import { inferTemplate } from '../../src/infer/inferTemplate.js';
import { closePool, getPool } from '../../src/persistence/db.js';
import {
  getTemplate,
  saveTemplate,
} from '../../src/persistence/templateRepo.js';
import { Template } from '../../src/types.js';

const testDatabaseUrl = process.env.TEST_DATABASE_URL;
const integrationDescribe = testDatabaseUrl ? describe : describe.skip;
const examplePath = path.resolve(
  __dirname,
  '../../project-docs/cvs-exemplo/Portfolio analise diretoria.txt',
);
const originalEnvironment = {
  databaseUrl: process.env.DATABASE_URL,
  databaseSchema: process.env.DATABASE_SCHEMA,
  databaseSsl: process.env.DATABASE_SSL,
};

function quoteIdentifier(identifier: string): string {
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(identifier)) {
    throw new Error(`Unexpected test identifier: ${identifier}`);
  }
  return `"${identifier}"`;
}

integrationDescribe('PostgreSQL template persistence and SQL execution', () => {
  let template: Template;
  let templateId: string | undefined;
  let testSchema: string;
  let schemaCreated = false;

  beforeAll(async () => {
    if (!testDatabaseUrl) {
      throw new Error(
        'TEST_DATABASE_URL is required for PostgreSQL integration tests.',
      );
    }
    const databaseUrl = new URL(testDatabaseUrl);
    const hostname = databaseUrl.hostname.replace(/^\[|\]$/g, '');
    const databaseName = decodeURIComponent(
      databaseUrl.pathname.replace(/^\//, ''),
    );
    if (
      !['localhost', '127.0.0.1', '::1'].includes(hostname) ||
      !/test/i.test(databaseName)
    ) {
      throw new Error(
        'Integration tests require a loopback PostgreSQL database with "test" in its name.',
      );
    }
    if (databaseUrl.searchParams.has('sslmode')) {
      throw new Error('Remove sslmode from TEST_DATABASE_URL.');
    }

    testSchema = `s696_it_${process.pid}`;
    databaseUrl.searchParams.set('options', `-c search_path=${testSchema}`);
    process.env.DATABASE_URL = databaseUrl.toString();
    process.env.DATABASE_SCHEMA = 'adm';
    process.env.DATABASE_SSL = 'disable';

    template = inferTemplate(
      parseCsvContent(readFileSync(examplePath, 'utf8')),
      'PostgreSQL integration test',
    );
    const pool = getPool();
    await pool.query(`CREATE SCHEMA ${quoteIdentifier(testSchema)}`);
    schemaCreated = true;
    await pool.query('CREATE SCHEMA IF NOT EXISTS adm');
    await pool.query(
      readFileSync(
        path.resolve(__dirname, '../../src/persistence/schema.sql'),
        'utf8',
      ),
    );
    for (const dataset of template.datasets) {
      const record = dataset.records[0];
      if (!record) {
        throw new Error(`Integration fixture has no ${dataset.key} record.`);
      }
      const foreignKeys = new Map(
        dataset.foreignKeys.map((foreignKey) => [
          foreignKey.column,
          foreignKey,
        ]),
      );
      const columns = Object.keys(record)
        .filter((column) => column !== dataset.primaryKey)
        .map(
          (column) =>
            `${quoteIdentifier(column)} ${foreignKeys.has(column) ? 'BIGINT' : 'TEXT'}`,
        );
      const constraints = dataset.foreignKeys.map((foreignKey) => {
        const parent = template.datasets.find(
          (candidate) => candidate.key === foreignKey.parentDataset,
        );
        if (!parent) {
          throw new Error(
            `Missing parent dataset ${foreignKey.parentDataset}.`,
          );
        }
        return `FOREIGN KEY (${quoteIdentifier(foreignKey.column)}) REFERENCES ${quoteIdentifier(testSchema)}.${quoteIdentifier(parent.targetTable)} (${quoteIdentifier(parent.primaryKey)})`;
      });
      await pool.query(
        `CREATE TABLE ${quoteIdentifier(testSchema)}.${quoteIdentifier(dataset.targetTable)} (${quoteIdentifier(dataset.primaryKey)} BIGSERIAL PRIMARY KEY, ${[...columns, ...constraints].join(', ')})`,
      );
    }
  });

  afterAll(async () => {
    try {
      if (templateId) {
        await getPool().query(
          'DELETE FROM adm.model_template WHERE template_id = $1',
          [templateId],
        );
      }
      if (schemaCreated) {
        await getPool().query(
          `DROP SCHEMA ${quoteIdentifier(testSchema)} CASCADE`,
        );
      }
    } finally {
      await closePool();
      if (originalEnvironment.databaseUrl === undefined) {
        delete process.env.DATABASE_URL;
      } else {
        process.env.DATABASE_URL = originalEnvironment.databaseUrl;
      }
      if (originalEnvironment.databaseSchema === undefined) {
        delete process.env.DATABASE_SCHEMA;
      } else {
        process.env.DATABASE_SCHEMA = originalEnvironment.databaseSchema;
      }
      if (originalEnvironment.databaseSsl === undefined) {
        delete process.env.DATABASE_SSL;
      } else {
        process.env.DATABASE_SSL = originalEnvironment.databaseSsl;
      }
    }
  });

  it('saves and reloads a template, then applies generated rows and relationships', async () => {
    templateId = await saveTemplate(template, 'integration-test.csv');
    const storedTemplate = await getTemplate(templateId);
    const preview = buildPreview(storedTemplate, {
      actionsCount: 2,
      stepsPerAction: 2,
    });
    const script = buildApplyScript(storedTemplate, preview.datasets);
    await getPool().query(script);

    const portfolio = await getPool().query(
      `SELECT COUNT(*)::int AS count FROM ${quoteIdentifier(template.datasets[0]!.targetTable)}`,
    );
    const actions = await getPool().query(
      `SELECT COUNT(*)::int AS count FROM "T696ACES"`,
    );
    const invalidStageLinks = await getPool().query(
      `SELECT COUNT(*)::int AS count
       FROM "T696ETAC" stage
       LEFT JOIN "T696ACES" action ON action."SQ_ACO_ETT" = stage."SQ_ACO_ETT"
       WHERE action."SQ_ACO_ETT" IS NULL`,
    );

    expect(portfolio.rows[0]?.count).toBe(1);
    expect(actions.rows[0]?.count).toBe(2);
    expect(invalidStageLinks.rows[0]?.count).toBe(0);
  });
});
