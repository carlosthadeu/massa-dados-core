import 'dotenv/config';
import { readFileSync } from 'node:fs';
import { PoolConfig } from 'pg';

/**
 * Expressão regular para validar identificadores seguros do PostgreSQL.
 * Garante que o identificador comece com uma letra ASCII ou sublinhado (_),
 * seguido por caracteres alfanuméricos ou sublinhados.
 */
const identifierPattern = /^[A-Za-z_][A-Za-z0-9_]*$/;

/**
 * Modelo de configuração de banco de dados da aplicação.
 */
export interface AppConfig {
  /**
   * O nome do esquema (schema) alvo no PostgreSQL.
   * O valor padrão é `'adm'`, caso não seja definido via variável de ambiente.
   */
  databaseSchema: string;

  /**
   * Opções de configuração repassadas diretamente ao construtor do `pg.Pool`.
   */
  pool: PoolConfig;
}

/**
 * Carrega, valida e constrói a configuração de banco de dados a partir de variáveis de ambiente.
 *
 * @remarks
 * Esta função analisa as seguintes variáveis de ambiente:
 * - `DATABASE_URL`: String de conexão obrigatória do PostgreSQL. Não deve conter o parâmetro de consulta `sslmode`.
 * - `DATABASE_SCHEMA`: Identificador opcional do schema. Padrão: `'adm'`. Deve seguir o padrão sintático de identificadores.
 * - `DATABASE_SSL`: Defina como `'disable'` para desativar o SSL. Permitido exclusivamente em conexões de loopback local (`localhost`, `127.0.0.1`, `::1`)
 *   com bancos de dados que contenham `'test'` no nome.
 * - `DATABASE_SSL_CA`: Caminho opcional no sistema de arquivos para o arquivo de certificado CA PEM.
 *
 * @returns A configuração resolvida {@link AppConfig} contendo as definições do schema e da pool.
 *
 * @throws {Error} Se `DATABASE_URL` não estiver definida.
 * @throws {Error} Se `DATABASE_URL` contiver o parâmetro `sslmode`.
 * @throws {Error} Se `DATABASE_SCHEMA` falhar na validação de formato do identificador.
 * @throws {Error} Se `DATABASE_SSL=disable` for usado em hosts fora do loopback ou em bases sem `'test'` no nome.
 * @throws {Error} Se `DATABASE_SSL_CA` for informado, mas o arquivo não puder ser lido.
 */
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