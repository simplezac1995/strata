// One-off: split pages/casino/wallet/sections.tsx (1,265 lines) into one file per wallet section.
//
// The file holds four exported section components plus the shared chain helpers, so the split is by declaration
// range. Import statements are rebuilt per output file from the source's import block (pruned to the names the
// chunk actually references) and a chunk-to-chunk import is added for the helpers each section uses; the original
// file becomes a barrel so the wallet pages keep importing the same specifier.
//
// Run: node scripts/split-wallet-sections.mjs
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const sourceFile = path.join(process.cwd(), "src/pages/casino/wallet/sections.tsx");
const outDir = path.join(process.cwd(), "src/pages/casino/wallet/sections");
const text = readFileSync(sourceFile, "utf8");
const lines = text.split("\n");
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
    .filter((entry) => entry !== null)
    .join("\n");

/** 1-based inclusive line ranges → text. */
const slice = (from, to) => lines.slice(from - 1, to).join("\n");

/** Chains the shared helpers: export the declarations the types chunk owns. */
const exportAll = (chunk) => chunk.replace(/^(type|interface|const|function) /gm, "export $1 ");

const chunks = [
  {
    file: "sections/types.ts",
    header: [
      "// Shared chain helpers for the hot/cold/history wallet sections.",
      "// Split out of sections.tsx so each section file can import them without importing its siblings.",
    ],
    ranges: [[52, 129]],
    shared: [
      "ChainKind",
      "ColdOp",
      "DepositOp",
      "CHAIN_PURPOSES",
      "CHAIN_NETWORK",
      "matchesChain",
      "depositOps",
    ],
  },
  {
    file: "sections/shell.tsx",
    header: ["// The page shell shared by every wallet tab (title + padded content area)."],
    ranges: [[130, 156]],
  },
  {
    file: "sections/hot.tsx",
    header: ["// Hot-wallet section: operations, balances and the per-purpose wallet lists."],
    ranges: [[157, 840]],
  },
  {
    file: "sections/cold.tsx",
    header: ["// Cold-wallet section and the cold-wallet card it renders."],
    ranges: [[841, 1202]],
  },
  {
    file: "sections/history.tsx",
    header: ["// Wallet operation history (sweeps, deploys, transfers)."],
    ranges: [[1203, lines.length]],
  },
];

const sharedNames = chunks[0].shared;
mkdirSync(outDir, { recursive: true });

for (const chunk of chunks) {
  const isTypes = chunk === chunks[0];
  const body = chunk.ranges.map(([from, to]) => slice(from, to)).join("\n\n");
  const usedShared = isTypes ? [] : sharedNames.filter((name) => refs(body, name));
  const sharedImport =
    usedShared.length > 0 ? `import { ${usedShared.join(", ")} } from "./types";` : "";
  writeFileSync(
    path.join(path.dirname(sourceFile), chunk.file),
    [...chunk.header, "", importsFor(body), sharedImport, "", isTypes ? exportAll(body) : body, ""]
      .filter((line, index, array) => !(line === "" && array[index - 1] === ""))
      .join("\n"),
    "utf8",
  );
}

// The original module becomes a barrel: every consumer keeps its existing import path.
writeFileSync(
  sourceFile,
  [
    "// Barrel: the wallet sections live one per file under ./sections/.",
    "// Kept so the tab pages keep importing '@/pages/casino/wallet/sections'.",
    "",
    'export * from "./sections/types";',
    'export * from "./sections/shell";',
    'export * from "./sections/hot";',
    'export * from "./sections/cold";',
    'export * from "./sections/history";',
    "",
  ].join("\n"),
  "utf8",
);

console.log("sections.tsx -> sections/{types,shell,hot,cold,history}");
