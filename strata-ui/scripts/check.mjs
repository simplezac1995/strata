/**
 * CI-friendly quality gate with explicit step labels (avoids silent && failures on Windows).
 */
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const isWindows = process.platform === "win32";

const steps = [
  "typecheck",
  "lint",
  "unused",
  "test",
  "i18n:verify",
  "i18n:scan",
  "format:check",
  "build",
  "size",
];

for (const step of steps) {
  console.log(`\n[check] ${step}`);
  const result = spawnSync(
    isWindows ? (process.env.ComSpec ?? "cmd.exe") : "pnpm",
    isWindows ? ["/d", "/s", "/c", `pnpm run ${step}`] : ["run", step],
    {
      cwd: root,
      stdio: "inherit",
    },
  );
  if (result.error) {
    console.error(`\n[check] failed to start "${step}": ${result.error.message}`);
    process.exit(1);
  }
  if (result.status !== 0) {
    console.error(`\n[check] failed at step "${step}" (exit ${result.status ?? "signal"})`);
    process.exit(result.status ?? 1);
  }
}

console.log("\n[check] all steps passed");
