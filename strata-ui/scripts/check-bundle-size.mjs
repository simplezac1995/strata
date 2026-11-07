// SPDX-License-Identifier: Apache-2.0
/**
 * Bundle size gate (runs after `vite build`).
 *
 * Fails CI when any single JS chunk exceeds the budget so heavyweight libraries
 * (e.g. a full ECharts import) can never silently slip back into a route chunk.
 * Pair with manualChunks in vite.config.ts and route-level React.lazy.
 */
import { existsSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const assetsDir = path.join(root, "dist", "assets");

// Hard ceiling for a single JS chunk (kB), aligned with PERFORMANCE_BUDGETS.md
// ("单个 chunk ≤ 600 kB"; bpmn-js stays under it as a lazy chunk).
const MAX_CHUNK_KB = 600;

if (!existsSync(assetsDir)) {
  console.error("[size] dist/assets not found — run `pnpm build` first");
  process.exit(1);
}

const chunks = readdirSync(assetsDir)
  .filter((f) => f.endsWith(".js"))
  .map((f) => ({ name: f, kb: statSync(path.join(assetsDir, f)).size / 1024 }))
  .sort((a, b) => b.kb - a.kb);

console.log("[size] largest JS chunks (kB):");
for (const c of chunks.slice(0, 10)) {
  console.log(`  ${c.kb.toFixed(1).padStart(9)}  ${c.name}`);
}

const offenders = chunks.filter((c) => c.kb > MAX_CHUNK_KB);
if (offenders.length > 0) {
  console.error(
    `\n[size] FAIL: ${offenders.length} chunk(s) exceed ${MAX_CHUNK_KB} kB: ` +
      offenders.map((c) => c.name).join(", "),
  );
  process.exit(1);
}

console.log(`\n[size] PASS: ${chunks.length} JS chunks, all <= ${MAX_CHUNK_KB} kB`);
