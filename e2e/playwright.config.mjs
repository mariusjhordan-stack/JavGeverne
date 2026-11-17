import { defineConfig } from 'playwright/test';

// The stack must already be running: see scripts/e2e.sh, which starts PostgreSQL-backed services,
// the Mercado Pago test double and the storefront, then runs this suite.
export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  workers: 1,
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'off',
    screenshot: 'only-on-failure',
  },
  outputDir: '../.run/e2e-results',
});
