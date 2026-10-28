if (!process.env.TEST_DATABASE_URL) {
  console.error(
    'TEST_DATABASE_URL is required. Start the disposable database with docker compose and set the local test URL first.',
  );
  process.exitCode = 1;
}
