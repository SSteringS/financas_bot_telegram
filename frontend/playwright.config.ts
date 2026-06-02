import { defineConfig } from '@playwright/test';
import { config as loadEnv } from 'dotenv';

// Carrega .env.e2e (silencioso se o arquivo não existir — OK em ambientes sem E2E configurado)
loadEnv({ path: '.env.e2e' });

export default defineConfig({
  testDir: './e2e/specs',
  globalSetup: './e2e/fixtures/global-setup.ts',
  outputDir: './playwright-report',
  reporter: [['html', { outputFolder: 'playwright-report' }], ['list']],
  use: {
    baseURL: process.env['E2E_FRONTEND_URL'] ?? 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        channel: 'chromium',
      },
    },
  ],
});
