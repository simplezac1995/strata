import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = path.join(root, "src");

const SHORT_KEY = /^[a-z][a-zA-Z0-9]*(?:\.[a-zA-Z0-9]+)+$/;
const ENGLISH_SENTENCE = /[A-Za-z]{3}/;

/** Design-system / demo pages — English labels are intentional. */
const SKIP_FILES = new Set([
  "pages/Components.tsx",
  "pages/Cover.tsx",
  "pages/Foundations.tsx",
  "pages/States.tsx",
  "pages/ErrorPages.tsx",
  "pages/Notifications.tsx",
  "App.tsx",
]);

function walk(dir, out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else out.push(full);
  }
  return out;
}

const issues = {
  tNonShort: [],
  subtitleLiteral: [],
  placeholderLiteral: [],
  jsxText: [],
};

for (const file of walk(src)) {
  if (!file.endsWith(".tsx") || file.includes("shared/i18n")) continue;
  const rel = path.relative(src, file).replace(/\\/g, "/");
  if (rel.includes("smoke.test") || rel.includes(".test.") || SKIP_FILES.has(rel)) continue;

  // Strip comments so jsdoc examples (e.g. `title="Sales by Category"` in chart demos)
  // are not treated as hardcoded UI strings.
  const text = readFileSync(file, "utf8")
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/\/\/.*$/gm, "");

  for (const m of text.matchAll(/(^|[^A-Za-z0-9_])t\(\s*['"]([^'"]+)['"]\s*\)/g)) {
    const k = m[2];
    if (k.includes("/") || k.startsWith(".")) continue;
    if (!SHORT_KEY.test(k) && ENGLISH_SENTENCE.test(k)) {
      issues.tNonShort.push({ file: rel, value: k });
    }
  }

  for (const m of text.matchAll(
    /(?:subtitle|title|description|hint|label)=\{?["']([A-Za-z][^"']{8,})["']/g,
  )) {
    if (SHORT_KEY.test(m[1])) continue;
    issues.subtitleLiteral.push({ file: rel, value: m[1] });
  }

  for (const m of text.matchAll(/placeholder=["']([A-Za-z][^"']{4,})["']/g)) {
    const v = m[1];
    if (/@|\.com|https?:|^\w+_\w+$|^[A-Z][A-Z0-9_]+$/.test(v)) continue;
    if (/^[a-z][a-z0-9.-]+$/.test(v) && !/\s/.test(v)) continue;
    if (/^[A-Z]+-[A-Z]+-\d+$/.test(v)) continue;
    if (v.startsWith("system:") || v.startsWith("system/")) continue;
    if (v.startsWith("sys.")) continue;
    if (v.startsWith("localhost")) continue;
    issues.placeholderLiteral.push({ file: rel, value: v });
  }

  for (const m of text.matchAll(/>\s*([A-Z][a-z]+(?:\s+[a-z]+){2,}[^<]{0,60})\s*</g)) {
    const v = m[1].trim();
    if (v.includes("t(") || v.includes("{")) continue;
    if (/[\u4e00-\u9fff]/.test(v)) continue;
    issues.jsxText.push({ file: rel, value: v });
  }
}

const outPath = path.join(root, "scripts/hardcoded-ui-scan.json");
writeFileSync(outPath, JSON.stringify(issues, null, 2));

const total =
  issues.tNonShort.length +
  issues.subtitleLiteral.length +
  issues.placeholderLiteral.length +
  issues.jsxText.length;

console.log("tNonShort:", issues.tNonShort.length);
console.log("subtitleLiteral:", issues.subtitleLiteral.length);
console.log("placeholderLiteral:", issues.placeholderLiteral.length);
console.log("jsxText:", issues.jsxText.length);
console.log("written:", outPath);

if (total > 0) {
  console.error(`hardcoded-ui-scan: ${total} issue(s) — fix or add i18n keys`);
  process.exit(1);
}
