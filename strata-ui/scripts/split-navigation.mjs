// One-off: split components/shell/navigation.ts (1,297 lines, of which 1,096 are the NAV table) into one data file
// per top-level navigation group. The array delimits its groups with `{ type: "separator", label: "nav.<group>" }`,
// so the split follows those markers and preserves their order; navigation.ts keeps the helper functions and
// re-assembles the array from the imported groups.
//
// Run: node scripts/split-navigation.mjs
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const sourceFile = path.join(process.cwd(), "src/components/shell/navigation.ts");
const outDir = path.join(process.cwd(), "src/components/shell/nav");
const text = readFileSync(sourceFile, "utf8");
const source = ts.createSourceFile(sourceFile, text, ts.ScriptTarget.Latest, true);

const imports = source.statements.filter(ts.isImportDeclaration).map((statement) => ({
  statement,
  text: text.slice(statement.getStart(source), statement.getEnd()),
}));
const refs = (chunk, name) => new RegExp(`\\b${name}\\b`).test(chunk);

function renderImport(declaration, chunk) {
  const clause = declaration.statement.importClause;
  if (!clause) return declaration.text;
  const parts = [];
  if (clause.name && refs(chunk, clause.name.text)) parts.push(clause.name.text);
  if (clause.namedBindings) {
    if (ts.isNamespaceImport(clause.namedBindings)) {
      const name = clause.namedBindings.name.text;
      if (refs(chunk, name)) parts.push(`* as ${name}`);
    } else {
      const specifiers = clause.namedBindings.elements
        .filter((element) => refs(chunk, (element.name ?? element.propertyName).text))
        .map((element) => element.getText(source));
      if (specifiers.length > 0) parts.push(`{ ${specifiers.join(", ")} }`);
    }
  }
  if (parts.length === 0) return null;
  const specifier = declaration.statement.moduleSpecifier.getText(source);
  return `import ${clause.isTypeOnly ? "type " : ""}${parts.join(", ")} from ${specifier}`;
}
const importsFor = (chunk) =>
  imports
    .map((declaration) => renderImport(declaration, chunk))
    .filter(Boolean)
    .join("\n");

// Locate `export const NAV: NavEntry[] = [...]`.
let navNode = null;
for (const statement of source.statements) {
  if (!ts.isVariableStatement(statement)) continue;
  for (const declaration of statement.declarationList.declarations) {
    if (
      declaration.name.getText(source) === "NAV" &&
      declaration.initializer &&
      ts.isArrayLiteralExpression(declaration.initializer)
    ) {
      navNode = { statement, declaration, array: declaration.initializer };
    }
  }
}
if (!navNode) throw new Error("NAV array not found");

const elementText = (element) => text.slice(element.getStart(source), element.getEnd());

/** Reads `label` out of a separator entry so the group can be named after it. */
function separatorLabel(element) {
  if (!ts.isObjectLiteralExpression(element)) return null;
  let type = null;
  let label = null;
  for (const property of element.properties) {
    if (!ts.isPropertyAssignment(property)) continue;
    const key = property.name.getText(source).replace(/^["']|["']$/g, "");
    if (key === "type") type = property.initializer.getText(source).replace(/^["']|["']$/g, "");
    if (key === "label") label = property.initializer.getText(source).replace(/^["']|["']$/g, "");
  }
  return type === "separator" ? label : null;
}

const groups = [];
let current = null;
for (const element of navNode.array.elements) {
  const label = separatorLabel(element);
  if (label) {
    const name = label
      .split(".")
      .pop()
      .replace(/[^a-zA-Z0-9]+/g, "-")
      .toLowerCase();
    current = { name, label, elements: [element] };
    groups.push(current);
  } else if (current) {
    current.elements.push(element);
  } else {
    current = { name: "misc", label: null, elements: [element] };
    groups.push(current);
  }
}

mkdirSync(outDir, { recursive: true });
const constants = [];
for (const group of groups) {
  const constant = `NAV_${group.name.toUpperCase().replace(/-/g, "_")}`;
  const body = `export const ${constant}: NavEntry[] = [\n${group.elements.map(elementText).join(",\n")},\n];`;
  writeFileSync(
    path.join(outDir, `${group.name}.ts`),
    [
      `// Sidebar navigation entries for the "${group.label ?? group.name}" group, in display order.`,
      "",
      importsFor(body),
      "",
      body,
      "",
    ]
      .filter((line, index, array) => !(line === "" && array[index - 1] === ""))
      .join("\n"),
    "utf8",
  );
  constants.push({ constant, name: group.name, label: group.label });
}

// Rebuild navigation.ts: same prologue, NAV assembled from the groups, helpers untouched.
const navStart = navNode.declaration.getStart(source);
const navEnd = navNode.declaration.getEnd();
const before = text.slice(0, navStart);
const after = text.slice(navEnd);
const groupImports = constants
  .map(({ constant, name }) => `import { ${constant} } from "./nav/${name}";`)
  .join("\n");
const assembled = [
  "export const NAV: NavEntry[] = [",
  ...constants.map(({ constant }) => `  ...${constant},`),
  "];",
].join("\n");

writeFileSync(sourceFile, `${before}${assembled}${after.replace(/^\s*\n/, "\n")}`, "utf8");

// Put the group imports right after the existing import block.
const rebuilt = readFileSync(sourceFile, "utf8");
const lastImport = imports[imports.length - 1];
const insertAt = lastImport.statement.getEnd();
writeFileSync(
  sourceFile,
  `${rebuilt.slice(0, insertAt)}\n${groupImports}${rebuilt.slice(insertAt)}`,
  "utf8",
);

console.log(
  `NAV split into ${groups.length} groups: ${groups.map((group) => group.name).join(", ")}`,
);
