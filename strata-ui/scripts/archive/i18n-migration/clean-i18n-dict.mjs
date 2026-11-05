/**
 * Remove corrupt dictionary lines and bogus keys from zh.ts / en.ts.
 */
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const SHORT_KEY = /^[a-z][a-zA-Z0-9]*(?:\.[a-z][a-zA-Z0-9]*)+$/;

for (const name of ["zh", "en"]) {
  const file = path.join(root, `src/shared/i18n/${name}.ts`);
  const text = readFileSync(file, "utf8");
  const header = text.split("\n").slice(0, 2).join("\n");
  const entries = new Map();
  for (const m of text.matchAll(/^\s*"((?:\\.|[^"\\])*)":\s*"((?:\\.|[^"\\])*)",/gm)) {
    const k = m[1].replace(/\\"/g, '"');
    if (!SHORT_KEY.test(k)) continue;
    entries.set(k, m[2].replace(/\\"/g, '"'));
  }
  const lines = [header, `export const ${name}: Record<string, string> = {`];
  for (const [k, v] of [...entries.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
    lines.push(`  ${JSON.stringify(k)}: ${JSON.stringify(v)},`);
  }
  lines.push("};");
  writeFileSync(file, lines.join("\n") + "\n");
  console.log(`${name}: kept ${entries.size} keys`);
}
