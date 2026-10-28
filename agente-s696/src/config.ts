import 'dotenv/config';
import { readFileSync } from 'node:fs';
import { PoolConfig } from 'pg';

const identifierPattern = /^[A-Za-z_][A-Za-z0-9_]*$/;

export interface AppConfig {
  databaseSchema: string;
  pool: PoolConfig;
}

export function loadConfig(): AppConfig {
  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL must be set before saving a template.');
  }
  const parsedUrl = new URL(connectionString);
  if (parsedUrl.searchParams.has('sslmode')) {
    throw new Error(
      'Remove sslmode from DATABASE_URL; TLS is configured with certificate validation.',
    );
  }

  const databaseSchema = process.env.DATABASE_SCHEMA ?? 'adm';
  if (!identifierPattern.test(databaseSchema)) {
    throw new Error(
      'DATABASE_SCHEMA must be a valid PostgreSQL schema identifier.',
    );
  }

  const caPath = process.env.DATABASE_SSL_CA;
  let ssl: PoolConfig['ssl'] = { rejectUnauthorized: true };
  if (process.env.DATABASE_SSL === 'disable') {
    const host = parsedUrl.hostname.replace(/^\[|\]$/g, '');
    const databaseName = decodeURIComponent(
      parsedUrl.pathname.replace(/^\//, ''),
    );
    if (
      !['localhost', '127.0.0.1', '::1'].includes(host) ||
      !/test/i.test(databaseName)
    ) {
      throw new Error(
        'DATABASE_SSL=disable is allowed only for a loopback PostgreSQL database with "test" in its name.',
      );
    }
    ssl = false;
  }
  if (caPath) {
    try {
      ssl = { ca: readFileSync(caPath, 'utf8'), rejectUnauthorized: true };
    } catch (error) {
      throw new Error(`Unable to read DATABASE_SSL_CA at "${caPath}".`, {
        cause: error,
      });
    }
  }

  return {
    databaseSchema,
    pool: {
      connectionString,
      ssl,
    },
  };
}
