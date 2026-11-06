// Verify every short key used by call sites exists in the generated dictionary.
import { readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = path.join(root, "src");
const zhDir = path.join(src, "shared/i18n/dict/zh");
const zhText = readdirSync(zhDir)
  .filter((name) => name.endsWith(".ts"))
  .map((name) => readFileSync(path.join(zhDir, name), "utf8"))
  .join("\n");
const shortKeys = new Set([...zhText.matchAll(/^\s*"([^"]+)":/gm)].map((m) => m[1]));

function walk(dir, out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else out.push(full);
  }
  return out;
}

const SHORT_KEY = /^[a-z][a-zA-Z0-9]*(?:\.[a-zA-Z0-9]+)+$/;

function addKey(set, key) {
  if (!SHORT_KEY.test(key)) return;
  if (/^(text|primary|secondary|error|warning|info|success)\.[a-zA-Z0-9.]+$/.test(key)) return;
  set.add(key);
}

const Q = "(['\"])";
const used = new Set();
for (const file of walk(src)) {
  if (!/\.(tsx|ts)$/.test(file) || file.includes("shared/i18n")) continue;
  const text = readFileSync(file, "utf8");
  const tCall = new RegExp(`(?<![A-Za-z0-9_$])t\\(${Q}((?:\\\\.|(?!\\1).)*)\\1\\)`, "g");
  const toastCall = new RegExp(
    `(?<![A-Za-z0-9_$])toast\\.(?:success|error|info)\\(${Q}((?:\\\\.|(?!\\1).)*)\\1\\)`,
    "g",
  );
  const msgKey = new RegExp(`(?:success|failure):\\s*${Q}((?:\\\\.|(?!\\1).)*)\\1`, "g");
  for (const m of text.matchAll(tCall)) addKey(used, m[2]);
  for (const m of text.matchAll(toastCall)) addKey(used, m[2]);
  for (const m of text.matchAll(msgKey)) {
    const v = m[2];
    const looksCss = /(?:bg-|text-|border-|dark:|rounded-|px-|py-|flex |shadow)/.test(v);
    if (!looksCss && v.length > 1) addKey(used, v);
  }
  // Keys passed dynamically to t() (layout pickers, nav labels, etc.)
  const literalKey =
    /"(?:(?:text|nav|settings|error|common|auth|shell|ai|system|plugins|pro|tools|docs|pages|ui)\.[a-zA-Z0-9.]+)"/g;
  for (const m of text.matchAll(literalKey)) addKey(used, m[0].slice(1, -1));
}

const missing = [...used].filter((k) => !shortKeys.has(k));
console.log(`used=${used.size} missingFromDict=${missing.length}`);
if (missing.length) {
  console.log(missing.join("\n"));
  process.exit(1);
}
