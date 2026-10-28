import { closePool } from './persistence/db.js';
import { executarConhecimento } from './knowledge/cli.js';

async function main(): Promise<void> {
  try {
    process.exitCode = await executarConhecimento(process.argv.slice(2));
  } catch (error) {
    console.error(
      `Erro: ${error instanceof Error ? error.message : String(error)}`,
    );
    process.exitCode = 1;
  } finally {
    await closePool();
  }
}

void main();
