// SPDX-License-Identifier: Apache-2.0
import { defineConfig } from "@playwright/test";

/**
 * E2E configuration for the open-source minimal application: the workflow starts the backend and the Vite dev
 * server itself, so there is no webServer block here.
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 90_000,
  expect: { timeout: 15_000 },
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:5173",
    trace: "on-first-retry",
  },
});
