import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? 'html' : 'list',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'node .output/server/index.mjs',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
    env: {
      NUXT_PUBLIC_API_BASE_URL: 'http://localhost:8080/api/v1',
      NUXT_PUBLIC_KEYCLOAK_URL: 'http://localhost:8180',
      NUXT_PUBLIC_KEYCLOAK_REALM: 'auction-platform',
      NUXT_PUBLIC_KEYCLOAK_CLIENT_ID: 'buyer-web',
    },
  },
})
