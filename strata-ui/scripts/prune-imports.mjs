// One-off: prune imports in a file down to the names it actually references.
//
// Used after the navigation split: the NAV table (which used every icon) moved into ./nav/*, leaving 78 unused icon
// imports behind in navigation.ts. oxlint --deny-warnings is part of the gate, so they must go.
//
// Run: node scripts/prune-imports.mjs <file>
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const file = path.resolve(process.argv[2] ?? "");
const text = readFileSync(file, "utf8");
const source = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true);

const imports = source.statements.filter(ts.isImportDeclaration);
const firstImport = imports[0];
const lastImport = imports[imports.length - 1];
const body = text.slice(lastImport.getEnd());
const refs = (name) => new RegExp(`\\b${name}\\b`).test(body);

function render(declaration) {
  const clause = declaration.importClause;
  if (!clause) return declaration.getText(source);
  const parts = [];
  if (clause.name && refs(clause.name.text)) parts.push(clause.name.text);
  if (clause.namedBindings) {
    if (ts.isNamespaceImport(clause.namedBindings)) {
      const name = clause.namedBindings.name.text;
      if (refs(name)) parts.push(`* as ${name}`);
    } else {
      const kept = clause.namedBindings.elements
        .filter((element) => refs((element.name ?? element.propertyName).text))
        .map((element) => element.getText(source));
      if (kept.length > 0) parts.push(`{ ${kept.join(", ")} }`);
    }
  }
  if (parts.length === 0) return null;
  const specifier = declaration.moduleSpecifier.getText(source);
  return `import ${clause.isTypeOnly ? "type " : ""}${parts.join(", ")} from ${specifier}`;
}

const kept = imports
  .map(render)
  .filter((entry) => entry !== null)
  .join("\n");
const header = text.slice(0, firstImport.getStart(source));
writeFileSync(file, `${header}${kept}\n${body.replace(/^\s*\n/, "\n")}`, "utf8");
console.log(`pruned ${imports.length} import statements -> ${kept.split("\n").length}`);
