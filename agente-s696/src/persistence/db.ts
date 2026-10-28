import { Pool } from 'pg';
import { loadConfig } from '../config.js';

let pool: Pool | undefined;

export function getPool(): Pool {
  if (!pool) {
    pool = new Pool(loadConfig().pool);
  }
  return pool;
}

export async function closePool(): Promise<void> {
  if (pool) {
    const currentPool = pool;
    pool = undefined;
    await currentPool.end();
  }
}
