import { loadConfig } from '../src/config.js';

const environmentKeys = [
  'DATABASE_URL',
  'DATABASE_SCHEMA',
  'DATABASE_SSL',
  'DATABASE_SSL_CA',
] as const;
const originalEnvironment = Object.fromEntries(
  environmentKeys.map((key) => [key, process.env[key]]),
);

afterEach(() => {
  for (const key of environmentKeys) {
    const value = originalEnvironment[key];
    if (value === undefined) {
      delete process.env[key];
    } else {
      process.env[key] = value;
    }
  }
});

describe('database TLS configuration', () => {
  it('only allows explicitly disabled TLS for loopback connections', () => {
    process.env.DATABASE_URL = 'postgres://user:pass@db.example.com/app';
    process.env.DATABASE_SCHEMA = 'adm';
    process.env.DATABASE_SSL = 'disable';
    delete process.env.DATABASE_SSL_CA;

    expect(() => loadConfig()).toThrow(
      'DATABASE_SSL=disable is allowed only for a loopback PostgreSQL database with "test" in its name.',
    );
  });

  it('allows the PostgreSQL integration test to use a loopback connection', () => {
    process.env.DATABASE_URL =
      'postgres://user:pass@127.0.0.1:55432/agente_s696_test';
    process.env.DATABASE_SCHEMA = 'adm';
    process.env.DATABASE_SSL = 'disable';
    delete process.env.DATABASE_SSL_CA;

    expect(loadConfig().pool.ssl).toBe(false);
  });
});
