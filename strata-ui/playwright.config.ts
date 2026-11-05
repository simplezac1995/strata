import { defineConfig } from "@playwright/test";
import path from "node:path";
import { fileURLToPath } from "node:url";

const uiRoot = path.dirname(fileURLToPath(import.meta.url));
const backendRoot = path.resolve(uiRoot, "../strata");
const e2eApiPort = 18080;
const e2eUiPort = 15173;
// Actuator must share the API port in e2e: the standalone management context returns 500 for
// every actuator endpoint (observed on 8081), so a separate management port can never pass the
// health probe. One port keeps the probe stable and matches CI.
const e2eMgmtPort = e2eApiPort;
// CI pre-builds the backend in its own (unbounded) step; webServer then only waits for the JVM,
// keeping the health-check timeout small and stable.
const skipBackendBuild = process.env.STRATA_E2E_SKIP_BACKEND_BUILD === "1";

export default defineConfig({
  testDir: "./e2e",
  timeout: 90_000,
  expect: { timeout: 15_000 },
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: `http://127.0.0.1:${e2eUiPort}`,
    trace: "on-first-retry",
  },
  webServer: [
    {
      // Actuator must share the API port in e2e: the default separate management port
      // (8081) returns 500 for every actuator endpoint in the standalone-management context,
      // so Playwright's health probe would never succeed. One port keeps the probe stable.
      command: skipBackendBuild
        ? `java -jar strata-app/target/strata-app-0.1.0-SNAPSHOT.jar --server.port=${e2eApiPort} --management.server.port=${e2eApiPort}`
        : `mvn -q -pl strata-app -am package -DskipTests && java -jar strata-app/target/strata-app-0.1.0-SNAPSHOT.jar --server.port=${e2eApiPort} --management.server.port=${e2eApiPort}`,
      cwd: backendRoot,
      url: `http://127.0.0.1:${e2eMgmtPort}/actuator/health`,
      reuseExistingServer: false,
      // 60-module Maven build + application boot on a fresh runner can exceed 240s;
      // CI pre-builds and skips the Maven step here.
      timeout: skipBackendBuild ? 120_000 : 360_000,
      env: {
        STRATA_JWT_SECRET: "e2e-test-jwt-secret-at-least-32-chars-long",
        SERVER_PORT: String(e2eApiPort),
        STRATA_MANAGEMENT_PORT: String(e2eMgmtPort),
      },
    },
    {
      command: `pnpm exec vite --host 127.0.0.1 --port ${e2eUiPort} --strictPort`,
      cwd: uiRoot,
      url: `http://127.0.0.1:${e2eUiPort}`,
      reuseExistingServer: false,
      timeout: 120_000,
      env: {
        VITE_API_PROXY_TARGET: `http://127.0.0.1:${e2eApiPort}`,
      },
    },
  ],
});
