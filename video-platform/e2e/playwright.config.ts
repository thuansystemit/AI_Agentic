import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for the Video Platform E2E test suite.
 *
 * Frontend: http://localhost:4200  (Angular dev server)
 * Backend:  http://localhost:8080  (Spring Boot API)
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // Keep false: tests share the same backend DB state
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : undefined,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
    actionTimeout: 10_000,
    navigationTimeout: 15_000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],

  // Uncomment to start the Angular dev server automatically before tests:
  // webServer: {
  //   command: 'npm run start',
  //   cwd: '../frontend',
  //   url: 'http://localhost:4200',
  //   reuseExistingServer: !process.env['CI'],
  //   timeout: 120_000,
  // },
});
