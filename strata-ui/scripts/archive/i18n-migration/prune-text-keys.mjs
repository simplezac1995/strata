// Remove unused text.* keys from dictionaries (safe object-level prune).
import { readFileSync, writeFileSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = path.join(root, "src");
const enPath = path.join(src, "shared/i18n/en.ts");
const zhPath = path.join(src, "shared/i18n/zh.ts");
const keyMapPath = path.join(src, "shared/i18n/keyMap.ts");
const indexPath = path.join(src, "shared/i18n/index.tsx");

const SHORT_KEY = /^[a-z][a-zA-Z0-9]*(?:\.[a-zA-Z0-9]+)+$/;
const LITERAL_KEY =
  /"(?:(?:text|nav|settings|error|common|auth|shell|ai|system|plugins|pro|tools|docs|pages|ui)\.[a-zA-Z0-9.]+)"/g;

function addKey(set, key) {
  if (SHORT_KEY.test(key)) set.add(key);
}

function walk(dir, out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else out.push(full);
  }
  return out;
}

function parseDict(filePath) {
  const text = readFileSync(filePath, "utf8");
  const start = text.indexOf("{");
  const end = text.lastIndexOf("};");
  const body = text.slice(start, end + 1);
  return Function(`"use strict"; return (${body});`)();
}

function serializeDict(name, dict) {
  const keys = Object.keys(dict).sort((a, b) => a.localeCompare(b));
  const lines = keys.map((k) => {
    const v = dict[k];
    if (v.includes("\n")) return `  "${k}":\n    ${JSON.stringify(v)},`;
    return `  "${k}": ${JSON.stringify(v)},`;
  });
  return `// Short-key dictionary (${name === "en" ? "en" : "zh"}) — source of truth; batch fixes via scripts/zh-overrides.json + pnpm i18n:patch.\nexport const ${name}: Record<string, string> = {\n${lines.join("\n")}\n};\n`;
}

const used = new Set();
const Q = "(['\"])";
const tCall = new RegExp(`(?<![A-Za-z0-9_$])t\\(${Q}((?:\\\\.|(?!\\1).)*)\\1\\)`, "g");
const toastCall = new RegExp(
  `(?<![A-Za-z0-9_$])toast\\.(?:success|error|info)\\(${Q}((?:\\\\.|(?!\\1).)*)\\1\\)`,
  "g",
);
const msgKey = new RegExp(`(?:success|failure):\\s*${Q}((?:\\\\.|(?!\\1).)*)\\1`, "g");

for (const file of walk(src)) {
  if (!/\.(tsx|ts)$/.test(file)) continue;
  const rel = path.relative(src, file).replace(/\\/g, "/");
  if (rel === "shared/i18n/en.ts" || rel === "shared/i18n/zh.ts") continue;
  const text = readFileSync(file, "utf8");
  for (const m of text.matchAll(tCall)) addKey(used, m[2]);
  for (const m of text.matchAll(toastCall)) addKey(used, m[2]);
  for (const m of text.matchAll(msgKey)) {
    const v = m[2];
    const looksCss = /(?:bg-|text-|border-|dark:|rounded-|px-|py-|flex |shadow)/.test(v);
    if (!looksCss && v.length > 1) addKey(used, v);
  }
  for (const m of text.matchAll(LITERAL_KEY)) addKey(used, m[0].slice(1, -1));
}
for (const m of readFileSync(keyMapPath, "utf8").matchAll(/:\s*"([^"]+)"/g)) addKey(used, m[1]);
for (const m of readFileSync(indexPath, "utf8").matchAll(/:\s*"([^"]+)"/g)) addKey(used, m[1]);

function prune(filePath, name) {
  const dict = parseDict(filePath);
  let removed = 0;
  for (const key of Object.keys(dict)) {
    if (!key.startsWith("text.")) continue;
    if (used.has(key)) continue;
    delete dict[key];
    removed++;
  }
  writeFileSync(filePath, serializeDict(name, dict));
  return removed;
}

const enRemoved = prune(enPath, "en");
const zhRemoved = prune(zhPath, "zh");
const stillUsedText = [...used].filter((k) => k.startsWith("text."));
console.log(
  `used=${used.size} removed en=${enRemoved} zh=${zhRemoved} stillUsedText=${stillUsedText.length}`,
);
if (stillUsedText.length) console.log(stillUsedText.join("\n"));
