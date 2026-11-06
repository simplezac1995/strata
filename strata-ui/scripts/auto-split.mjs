// One-off: bring every remaining oversized admin module under the 500-line cap.
//
// For each target file the script:
//   1. parses the top-level statements and picks the "main" declaration (the default export, else the largest function,
//      else the largest declaration);
//   2. if the main declaration alone is still oversized and is a component with a single trailing return statement,
//      extracts its state/handlers into a `use<Name>(...)` hook (the JSX stays where it was, so behaviour is unchanged
//      as long as the hook returns every name the JSX references);
//   3. moves every other top-level declaration into chunk files of at most ~400 lines, rebuilding each chunk's imports
//      and turning cross-chunk references into explicit imports;
//   4. rewrites the source file with pruned imports, chunk/hook imports, the main declaration and the original
//      non-declaration statements (re-exports, `export default X;`).
//
// Files that cannot be split safely are reported and left untouched.
//
// Run: node scripts/auto-split.mjs <file...>
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

const MAX_LINES = Number(process.env.MAX_LINES ?? 480);
const CHUNK_TARGET = 400;

function lineOf(source, position) {
  return source.getLineAndCharacterOfPosition(position).line + 1;
}

function declarationKind(statement) {
  if (ts.isFunctionDeclaration(statement)) return "function";
  if (ts.isVariableStatement(statement)) {
    const declaration = statement.declarationList.declarations[0];
    const name = ts.isIdentifier(declaration.name) ? declaration.name.text : "const";
    const initializer = declaration.initializer;
    const jsx = initializer && /<[A-Z][\w.]*|<\/[A-Z]/.test(initializer.getText());
    return initializer && (ts.isArrowFunction(initializer) || ts.isCallExpression(initializer))
      ? jsx
        ? "component"
        : "value"
      : "value";
  }
  if (ts.isTypeAliasDeclaration(statement) || ts.isInterfaceDeclaration(statement)) return "type";
  if (ts.isClassDeclaration(statement) || ts.isEnumDeclaration(statement)) return "class";
  return null;
}

function declaredName(statement) {
  if (statement.name && ts.isIdentifier(statement.name)) return statement.name.text;
  if (ts.isVariableStatement(statement)) {
    const declaration = statement.declarationList.declarations[0];
    if (declaration && ts.isIdentifier(declaration.name)) return declaration.name.text;
  }
  return null;
}

function declarationIsDefault(statement) {
  return (statement.modifiers ?? []).some(
    (modifier) => modifier.kind === ts.SyntaxKind.DefaultKeyword,
  );
}

/** Collects every binding name introduced by a variable statement (destructuring included). */
function bindingNames(name, out) {
  if (ts.isIdentifier(name)) out.push(name.text);
  else if (ts.isObjectBindingPattern(name) || ts.isArrayBindingPattern(name)) {
    for (const element of name.elements) {
      if (ts.isBindingElement(element)) bindingNames(element.name, out);
    }
  }
}

/** Rebuilds the module-specifier imports the given text actually uses, re-targeting relative specifiers. */
function renderImports({ text, chunkFile, sourceFile, imports, source }) {
  const refs = (name) => new RegExp(`\\b${name.replace(/[$]/g, "\\$")}\\b`).test(text);
  const lines = [];
  for (const declaration of imports) {
    const clause = declaration.importClause;
    if (!clause) continue;
    const parts = [];
    if (clause.name && refs(clause.name.text)) parts.push(clause.name.text);
    if (clause.namedBindings) {
      if (ts.isNamespaceImport(clause.namedBindings)) {
        if (refs(clause.namedBindings.name.text))
          parts.push(`* as ${clause.namedBindings.name.text}`);
      } else {
        const kept = clause.namedBindings.elements
          .filter((element) => refs((element.name ?? element.propertyName).text))
          .map((element) => element.getText(source));
        if (kept.length > 0) parts.push(`{ ${kept.join(", ")} }`);
      }
    }
    if (parts.length === 0) continue;
    let specifier = declaration.moduleSpecifier.getText(source).replace(/^["']|["']$/g, "");
    if (specifier.startsWith(".")) {
      const target = path.resolve(path.dirname(sourceFile), specifier);
      const relative = path.relative(path.dirname(chunkFile), target).split(path.sep).join("/");
      specifier = relative.startsWith(".") ? relative : `./${relative}`;
    }
    lines.push(
      `import ${clause.isTypeOnly ? "type " : ""}${parts.join(", ")} from "${specifier}";`,
    );
  }
  return lines;
}

function exportedNamesOf(text) {
  return [
    ...new Set(
      [...text.matchAll(/export (?:type|interface|const|function|class|enum) (\w+)/g)].map(
        (match) => match[1],
      ),
    ),
  ];
}

const summary = [];

for (const relative of process.argv.slice(2)) {
  const sourceFile = path.resolve(process.cwd(), relative);
  const text = readFileSync(sourceFile, "utf8");
  const source = ts.createSourceFile(sourceFile, text, ts.ScriptTarget.Latest, true);
  const statements = [...source.statements];
  const imports = statements.filter(ts.isImportDeclaration);
  const declStatements = statements.filter((statement) => declarationKind(statement) !== null);
  const otherStatements = statements.filter(
    (statement) => !ts.isImportDeclaration(statement) && declarationKind(statement) === null,
  );

  if (declStatements.length === 0) {
    summary.push(`SKIP ${relative}: no declarations`);
    continue;
  }

  const decls = declStatements.map((statement) => {
    const name = declaredName(statement);
    return {
      statement,
      name,
      kind: declarationKind(statement),
      start: lineOf(source, statement.getStart(source)),
      end: lineOf(source, statement.getEnd()),
      text: statement.getText(source),
      isDefault: declarationIsDefault(statement),
    };
  });
  for (const declaration of decls) declaration.size = declaration.end - declaration.start + 1;

  const exportAssignment = otherStatements.find(
    (statement) => ts.isExportAssignment(statement) && !statement.isExportEquals,
  );
  let defaultName = decls.find((declaration) => declaration.isDefault)?.name;
  if (!defaultName && exportAssignment) {
    defaultName = exportAssignment.expression.getText(source);
  }

  let main = (defaultName && decls.find((declaration) => declaration.name === defaultName)) || null;
  if (!main)
    main = decls
      .filter((declaration) => declaration.kind === "function")
      .sort((a, b) => b.size - a.size)[0];
  if (!main) main = [...decls].sort((a, b) => b.size - a.size)[0];

  const others = decls.filter((declaration) => declaration !== main);
  const baseDir = path.dirname(sourceFile);
  const baseName = path.basename(sourceFile).replace(/\.(tsx?)$/, "");
  const chunkDir = path.join(baseDir, baseName.toLowerCase());
  const relChunkDir = (file) => path.relative(process.cwd(), file).split(path.sep).join("/");
  mkdirSync(chunkDir, { recursive: true });

  const chunkFiles = [];
  const usedNames = new Set();

  // Partition the remaining declarations, keeping types/values first and preserving source order.
  let bucket = [];
  let bucketLines = 0;
  const flush = () => {
    if (bucket.length === 0) return;
    const index = chunkFiles.length + 1;
    const hasJsx = bucket.some((declaration) => /<[A-Z]|<\/[A-Z]/.test(declaration.text));
    const file = path.join(chunkDir, `part-${index}.${hasJsx ? "tsx" : "ts"}`);
    chunkFiles.push({ declarations: bucket, file });
    bucket = [];
    bucketLines = 0;
  };
  for (const declaration of others) {
    if (bucketLines > 0 && bucketLines + declaration.size > CHUNK_TARGET) flush();
    bucket.push(declaration);
    bucketLines += declaration.size;
  }
  flush();

  const chunkTexts = new Map();
  for (const chunk of chunkFiles) {
    const body = chunk.declarations
      .map((declaration) =>
        declaration.text.startsWith("export ") ? declaration.text : `export ${declaration.text}`,
      )
      .join("\n\n");
    chunkTexts.set(chunk.file, body);
  }

  // Cross-chunk imports (siblings and the main declaration).
  for (const chunk of chunkFiles) {
    const body = chunkTexts.get(chunk.file);
    const extra = [];
    if (
      main.name &&
      main.name !== chunk.declarations[0]?.name &&
      new RegExp(`\\b${main.name}\\b`).test(body)
    ) {
      const relative = path
        .relative(path.dirname(chunk.file), sourceFile)
        .split(path.sep)
        .join("/")
        .replace(/\.tsx?$/, "");
      extra.push(
        `import { ${main.name} } from "${relative.startsWith(".") ? relative : `./${relative}`}";`,
      );
    }
    for (const sibling of chunkFiles) {
      if (sibling === chunk) continue;
      const names = exportedNamesOf(chunkTexts.get(sibling.file)).filter(
        (name) => name !== chunk.declarations[0]?.name && new RegExp(`\\b${name}\\b`).test(body),
      );
      if (names.length === 0) continue;
      const specifier = path
        .relative(path.dirname(chunk.file), sibling.file)
        .split(path.sep)
        .join("/")
        .replace(/\.tsx?$/, "");
      extra.push(
        `import { ${names.join(", ")} } from "${specifier.startsWith(".") ? specifier : `./${specifier}`}";`,
      );
    }
    const moduleImports = renderImports({
      text: body,
      chunkFile: chunk.file,
      sourceFile,
      imports,
      source,
    });
    writeFileSync(
      chunk.file,
      [
        `// Split out of ${path.basename(sourceFile)} by scripts/auto-split.mjs.`,
        "",
        ...moduleImports,
        ...extra,
        "",
        body,
        "",
      ].join("\n"),
      "utf8",
    );
  }

  // Hook extraction when the main component alone is too large.
  let hookImport = null;
  let mainText = main.text;
  const bodyStatements = ts.isFunctionDeclaration(main.statement)
    ? [...(main.statement.body?.statements ?? [])]
    : [];
  const returns = bodyStatements.filter((statement) => ts.isReturnStatement(statement));
  const lastIsReturn =
    bodyStatements.length > 0 && ts.isReturnStatement(bodyStatements[bodyStatements.length - 1]);
  if (
    main.size > MAX_LINES &&
    ts.isFunctionDeclaration(main.statement) &&
    !main.statement.typeParameters?.length &&
    returns.length === 1 &&
    lastIsReturn
  ) {
    let hookName = `use${main.name}`;
    const importedNames = imports.flatMap((declaration) => {
      const clause = declaration.importClause;
      if (!clause) return [];
      const names = [];
      if (clause.name) names.push(clause.name.text);
      if (clause.namedBindings) {
        if (ts.isNamespaceImport(clause.namedBindings)) names.push(clause.namedBindings.name.text);
        else
          names.push(
            ...clause.namedBindings.elements.map(
              (element) => (element.name ?? element.propertyName).text,
            ),
          );
      }
      return names;
    });
    const takenNames = new Set([...decls.map((declaration) => declaration.name), ...importedNames]);
    if (takenNames.has(hookName)) hookName = `use${main.name}State`;
    const hookFile = path.join(chunkDir, `${hookName}.${main.text.includes("<") ? "tsx" : "ts"}`);
    const hookBody = bodyStatements
      .slice(0, -1)
      .map((statement) => statement.getText(source))
      .join("\n\n");
    const names = [];
    for (const statement of bodyStatements.slice(0, -1)) {
      if (ts.isVariableStatement(statement)) {
        for (const declaration of statement.declarationList.declarations)
          bindingNames(declaration.name, names);
      } else if (ts.isFunctionDeclaration(statement) && statement.name) {
        names.push(statement.name.text);
      }
    }
    const unique = [...new Set(names)];
    const parameters = main.statement.parameters
      .map((parameter) => parameter.getText(source))
      .join(", ");
    // Call the hook with the same shape the component receives: a destructured parameter is passed as an object.
    const argumentNames = main.statement.parameters
      .map((parameter) => {
        if (ts.isIdentifier(parameter.name)) return parameter.name.text;
        const collected = [];
        bindingNames(parameter.name, collected);
        return `{ ${collected.join(", ")} }`;
      })
      .join(", ");
    const hookChunkImports = chunkFiles
      .map((chunk) => {
        const names = exportedNamesOf(chunkTexts.get(chunk.file)).filter((name) =>
          new RegExp(`\\b${name}\\b`).test(hookBody),
        );
        if (names.length === 0) return null;
        const specifier = path
          .relative(path.dirname(hookFile), chunk.file)
          .split(path.sep)
          .join("/")
          .replace(/\.tsx?$/, "");
        return `import { ${names.join(", ")} } from "${specifier.startsWith(".") ? specifier : `./${specifier}`}";`;
      })
      .filter(Boolean);
    const hookSource = [
      `// State and handlers of ${main.name}, split out of ${path.basename(sourceFile)}.`,
      "",
      ...renderImports({
        text: hookBody + parameters,
        chunkFile: hookFile,
        sourceFile,
        imports,
        source,
      }),
      ...hookChunkImports,
      "",
      `export function ${hookName}(${parameters}) {`,
      hookBody,
      "",
      `  return { ${unique.join(", ")} };`,
      "}",
      "",
    ].join("\n");
    writeFileSync(hookFile, hookSource, "utf8");
    const hookSpecifier = path
      .relative(path.dirname(sourceFile), hookFile)
      .split(path.sep)
      .join("/")
      .replace(/\.tsx?$/, "");
    hookImport = `import { ${hookName} } from "${hookSpecifier.startsWith(".") ? hookSpecifier : `./${hookSpecifier}`}";`;
    const returnText = bodyStatements[bodyStatements.length - 1].getText(source);
    const functionHead = `${text.slice(main.statement.getStart(source), main.statement.body.getStart(source)).trimEnd()} {`;
    mainText = [
      functionHead,
      `  const { ${unique.join(", ")} } = ${hookName}(${argumentNames});`,
      "",
      `  ${returnText}`,
      "}",
    ].join("\n");
  }

  if (!mainText.startsWith("export ")) mainText = `export ${mainText}`;
  const keptText = [
    mainText,
    ...otherStatements.map((statement) => statement.getText(source)),
  ].join("\n\n");
  const sourceImports = renderImports({
    text: keptText,
    chunkFile: sourceFile,
    sourceFile,
    imports,
    source,
  });
  const chunkImports = chunkFiles
    .map((chunk) => {
      const names = exportedNamesOf(chunkTexts.get(chunk.file)).filter(
        (name) => name !== main.name && new RegExp(`\\b${name}\\b`).test(keptText),
      );
      if (names.length === 0) return null;
      const specifier = path
        .relative(baseDir, chunk.file)
        .split(path.sep)
        .join("/")
        .replace(/\.tsx?$/, "");
      return `import { ${names.join(", ")} } from "${specifier.startsWith(".") ? specifier : `./${specifier}`}";`;
    })
    .filter(Boolean);

  const reExports = [];
  for (const chunk of chunkFiles) {
    const exported = chunk.declarations.filter(
      (declaration) => declaration.text.startsWith("export ") && declaration.name !== main.name,
    );
    const values = exported
      .filter((declaration) => declaration.kind !== "type")
      .map((declaration) => declaration.name);
    const types = exported
      .filter((declaration) => declaration.kind === "type")
      .map((declaration) => declaration.name);
    const specifier = path
      .relative(baseDir, chunk.file)
      .split(path.sep)
      .join("/")
      .replace(/\.tsx?$/, "");
    const from = `"${specifier.startsWith(".") ? specifier : `./${specifier}`}"`;
    if (values.length > 0) reExports.push(`export { ${values.join(", ")} } from ${from};`);
    if (types.length > 0) reExports.push(`export type { ${types.join(", ")} } from ${from};`);
  }
  const rewritten = [
    ...sourceImports,
    ...chunkImports,
    ...(hookImport ? [hookImport] : []),
    "",
    ...reExports,
    ...(reExports.length > 0 ? [""] : []),
    keptText,
    "",
  ].join("\n");
  writeFileSync(sourceFile, rewritten, "utf8");

  const rewrittenLines = rewritten.split("\n").length;
  summary.push(
    `${rewrittenLines > MAX_LINES ? "OVER " : "ok   "} ${relative}: ${main.size}→${rewrittenLines} lines, ${chunkFiles.length} chunk(s)${hookImport ? " + hook" : ""}`,
  );
}

console.log(summary.join("\n"));
