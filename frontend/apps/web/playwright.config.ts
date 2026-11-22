import { defineConfig, devices } from "@playwright/test"

/**
 * End-to-end tests against a production build of the web app and the real
 * local backend. Setup and environment: e2e/README.md.
 */
const PORT = Number(process.env.E2E_PORT ?? 3100)
const baseURL = process.env.E2E_BASE_URL ?? `http://localhost:${PORT}`

export default defineConfig({
  testDir: "e2e",
  // One worker: the backend allows ~5 requests per second per IP, and the
  // flows share its throttles (one code email per address per minute)
  workers: 1,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL,
    locale: "en-US",
    colorScheme: "light",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        // PLAYWRIGHT_CHANNEL=msedge (or chrome) reuses an installed browser
        channel: process.env.PLAYWRIGHT_CHANNEL || undefined,
      },
    },
  ],
  webServer: {
    command: `pnpm build && pnpm exec next start --port ${PORT}`,
    url: `${baseURL}/en/login`,
    reuseExistingServer: !process.env.CI,
    timeout: 300_000,
  },
})
