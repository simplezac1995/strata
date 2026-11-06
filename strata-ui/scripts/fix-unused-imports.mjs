// One-off: remove imports that oxlint reports as unused.
//
// Reads `npx oxlint --format json` output and, for every "Identifier 'X' is imported but never used" diagnostic,
// rebuilds that file's import statements without X (dropping the statement entirely when nothing is left). Only the
// identifiers oxlint names are touched, so default imports, type-only clauses, aliases and re-exports survive.
//
// Run: node scripts/fix-unused-imports.mjs <oxlint-json>
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const report = JSON.parse(readFileSync(path.resolve(process.argv[2]), "utf8"));
const root = process.cwd();
const byFile = new Map();

for (const diagnostic of report.diagnostics ?? []) {
  if (diagnostic.code !== "eslint(no-unused-vars)") continue;
  const match = /Identifier '(\w+)' is imported but never used/.exec(diagnostic.message ?? "");
  if (!match) continue;
  const file = path.resolve(root, diagnostic.filename);
  if (!byFile.has(file)) byFile.set(file, new Set());
  byFile.get(file).add(match[1]);
}

let touched = 0;
for (const [file, names] of byFile) {
  const text = readFileSync(file, "utf8");
  const source = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true);
  const edits = [];

  for (const statement of source.statements) {
    if (!ts.isImportDeclaration(statement)) continue;
    const clause = statement.importClause;
    if (!clause) continue;
    const parts = [];
    let removed = false;

    if (clause.name) {
      if (names.has(clause.name.text)) removed = true;
      else parts.push(clause.name.text);
    }
    if (clause.namedBindings) {
      if (ts.isNamespaceImport(clause.namedBindings)) {
        if (names.has(clause.namedBindings.name.text)) removed = true;
        else parts.push(`* as ${clause.namedBindings.name.text}`);
      } else {
        const kept = clause.namedBindings.elements.filter((element) => {
          const name = (element.name ?? element.propertyName).text;
          if (names.has(name)) {
            removed = true;
            return false;
          }
          return true;
        });
        if (kept.length > 0)
          parts.push(`{ ${kept.map((element) => element.getText(source)).join(", ")} }`);
      }
    }

    if (!removed) continue;
    const specifier = statement.moduleSpecifier.getText(source);
    const replacement =
      parts.length > 0
        ? `import ${clause.isTypeOnly ? "type " : ""}${parts.join(", ")} from ${specifier};`
        : null;
    edits.push({ start: statement.getStart(source), end: statement.getEnd(), replacement });
  }

  let next = text;
  for (const edit of edits.sort((a, b) => b.start - a.start)) {
    next = `${next.slice(0, edit.start)}${edit.replacement ?? ""}${next.slice(edit.end)}`;
  }
  if (next !== text) {
    writeFileSync(file, next, "utf8");
    touched += 1;
    console.log(`cleaned ${path.relative(root, file)}: ${[...names].join(", ")}`);
  }
}

console.log(`files touched: ${touched}`);
