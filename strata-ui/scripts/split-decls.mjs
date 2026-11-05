// Generic declaration-range splitter for the admin console.
//
// Reads a JSON manifest and moves top-level declaration ranges out of one source file into chunk files. Each chunk's
// module imports are rebuilt from the source's import block (pruned to what it references) and relative specifiers are
// re-targeted for the chunk's directory. Cross-chunk references are turned into explicit imports between siblings, so
// splitting a module whose declarations reference each other still compiles. The source file can become a barrel.
//
// Manifest: { source, barrel?, chunks: [{ file, ranges: [[from1Based, to1Based], ...], header?: string[] }] }
//
// Run: node scripts/split-decls.mjs <manifest.json>
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const manifest = JSON.parse(readFileSync(path.resolve(process.argv[2]), "utf8"));
const sourceFile = path.resolve(process.cwd(), manifest.source);
const text = readFileSync(sourceFile, "utf8");
const lines = text.split("\n");
const source = ts.createSourceFile(sourceFile, text, ts.ScriptTarget.Latest, true);

const imports = source.statements.filter(ts.isImportDeclaration).map((statement) => ({
  statement,
  text: text.slice(statement.getStart(source), statement.getEnd()),
}));
const refs = (chunk, name) => new RegExp(`\\b${name}\\b`).test(chunk);
const slice = (from, to) => lines.slice(from - 1, to).join("\n");
const exportAll = (chunk) =>
  chunk.replace(/^(type|interface|const|function|class|enum) /gm, "export $1 ");
const exportedNames = (chunk) => [
  ...new Set(
    [...chunk.matchAll(/export (?:type|interface|const|function|class|enum) (\w+)/g)].map(
      (match) => match[1],
    ),
  ),
];
const relativeSpecifier = (fromDir, target) => {
  const relative = path.relative(fromDir, target).split(path.sep).join("/");
  return relative.startsWith(".") ? relative : `./${relative}`;
};
const retarget = (specifier, chunkFile) => {
  if (!specifier.startsWith(".")) return specifier;
  return relativeSpecifier(
    path.dirname(chunkFile),
    path.resolve(path.dirname(sourceFile), specifier),
  );
};

function renderImport(declaration, chunk, chunkFile) {
  const clause = declaration.statement.importClause;
  if (!clause) return declaration.text;
  const parts = [];
  if (clause.name && refs(chunk, clause.name.text)) parts.push(clause.name.text);
  if (clause.namedBindings) {
    if (ts.isNamespaceImport(clause.namedBindings)) {
      const name = clause.namedBindings.name.text;
      if (refs(chunk, name)) parts.push(`* as ${name}`);
    } else {
      const kept = clause.namedBindings.elements
        .filter((element) => refs(chunk, (element.name ?? element.propertyName).text))
        .map((element) => element.getText(source));
      if (kept.length > 0) parts.push(`{ ${kept.join(", ")} }`);
    }
  }
  if (parts.length === 0) return null;
  const raw = declaration.statement.moduleSpecifier.getText(source).replace(/^["']|["']$/g, "");
  return `import ${clause.isTypeOnly ? "type " : ""}${parts.join(", ")} from "${retarget(raw, chunkFile)}"`;
}

// Pass 1: bodies, local declarations and exported names per chunk.
const prepared = manifest.chunks.map((chunk) => {
  const file = path.resolve(process.cwd(), chunk.file);
  const body = exportAll(chunk.ranges.map(([from, to]) => slice(from, to)).join("\n\n"));
  return { ...chunk, file, body, names: exportedNames(body) };
});

for (const chunk of prepared) {
  const moduleImports = imports
    .map((declaration) => renderImport(declaration, chunk.body, chunk.file))
    .filter((entry) => entry !== null);
  const siblingImports = [];
  for (const other of prepared) {
    if (other === chunk) continue;
    const used = other.names.filter((name) => refs(chunk.body, name));
    if (used.length === 0) continue;
    const specifier = relativeSpecifier(path.dirname(chunk.file), other.file).replace(
      /\.tsx?$/,
      "",
    );
    siblingImports.push(`import { ${used.join(", ")} } from "${specifier}";`);
  }
  mkdirSync(path.dirname(chunk.file), { recursive: true });
  const content = [
    ...(chunk.header ?? []),
    ...(chunk.header ? [""] : []),
    [...moduleImports, ...siblingImports].join("\n"),
    "",
    chunk.body,
    "",
  ]
    .filter((line, index, array) => !(line === "" && array[index - 1] === ""))
    .join("\n");
  writeFileSync(chunk.file, content, "utf8");
}

if (manifest.barrel) {
  const specifiers = prepared.map(
    (chunk) =>
      `export * from "${relativeSpecifier(path.dirname(sourceFile), chunk.file).replace(/\.tsx?$/, "")}";`,
  );
  writeFileSync(
    sourceFile,
    ["// Barrel — this module was split; the chunks live next to it.", "", ...specifiers, ""].join(
      "\n",
    ),
    "utf8",
  );
}

console.log(
  `split ${manifest.source} into ${prepared.length} chunk(s): ${prepared.map((chunk) => path.basename(chunk.file)).join(", ")}`,
);
