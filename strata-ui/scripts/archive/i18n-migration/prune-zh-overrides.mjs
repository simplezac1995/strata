// Drop legacy text.* overrides; rekey to semantic keys when en label matches.
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const overridesPath = path.join(root, "scripts/zh-overrides.json");
const enPath = path.join(root, "src/shared/i18n/en.ts");

function parseDict(filePath) {
  const text = readFileSync(filePath, "utf8");
  const start = text.indexOf("{");
  const end = text.lastIndexOf("};");
  return Function(`"use strict"; return (${text.slice(start, end + 1)});`)();
}

function rankKey(k) {
  if (k.startsWith("nav.")) return 0;
  if (k.startsWith("common.")) return 1;
  if (k.startsWith("error.")) return 2;
  if (k.startsWith("auth.")) return 3;
  if (k.startsWith("shell.")) return 4;
  if (k.startsWith("settings.")) return 5;
  return 6;
}

function pickBest(keys) {
  return [...keys].sort((a, b) => rankKey(a) - rankKey(b) || a.localeCompare(b))[0];
}

const en = parseDict(enPath);
const byEn = new Map();
for (const [k, v] of Object.entries(en)) {
  if (k.startsWith("text.")) continue;
  if (!byEn.has(v)) byEn.set(v, []);
  byEn.get(v).push(k);
}

const overrides = JSON.parse(readFileSync(overridesPath, "utf8"));
const next = {};
let dropped = 0;
let rekeyed = 0;
let kept = 0;

for (const [key, fix] of Object.entries(overrides)) {
  if (!key.startsWith("text.")) {
    next[key] = fix;
    kept++;
    continue;
  }
  const candidates = byEn.get(fix.en);
  if (!candidates?.length) {
    dropped++;
    continue;
  }
  const semantic = pickBest(candidates);
  if (next[semantic]) {
    dropped++;
    continue;
  }
  next[semantic] = fix;
  rekeyed++;
}

const ordered = Object.fromEntries(Object.entries(next).sort(([a], [b]) => a.localeCompare(b)));
writeFileSync(overridesPath, JSON.stringify(ordered, null, 2) + "\n");
console.log(
  `kept=${kept} rekeyed=${rekeyed} dropped=${dropped} total=${Object.keys(ordered).length}`,
);
