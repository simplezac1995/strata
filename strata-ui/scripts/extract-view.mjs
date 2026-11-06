// One-off: move a hook-driven page's JSX into its own view component without touching the JSX.
//
// Input must be a component whose body is a single hook call plus a single trailing return (the shape
// scripts/auto-split.mjs produces). The view receives the hook result as `view` and destructures it back into the
// original names, so the JSX is copied verbatim. The component's own props are forwarded too (a destructured
// parameter becomes individual props), because the JSX may reference them directly.
//
// Dialogs are optionally moved into a second file that receives the same view + props.
//
// Run: node scripts/extract-view.mjs <file>
import { existsSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import ts from "typescript";

function bindingNames(name, out) {
  if (ts.isIdentifier(name)) out.push(name.text);
  else if (ts.isObjectBindingPattern(name) || ts.isArrayBindingPattern(name)) {
    for (const element of name.elements)
      if (ts.isBindingElement(element)) bindingNames(element.name, out);
  }
}

for (const relative of process.argv.slice(2)) {
  const sourceFile = path.resolve(process.cwd(), relative);
  const text = readFileSync(sourceFile, "utf8");
  const source = ts.createSourceFile(sourceFile, text, ts.ScriptTarget.Latest, true);
  const imports = [...source.statements].filter(ts.isImportDeclaration);
  const refs = (target, name) => new RegExp(`\\b${name.replace(/[$]/g, "\\$")}\\b`).test(target);
  const baseDir = path.dirname(sourceFile);

  const functions = [...source.statements].filter(ts.isFunctionDeclaration);
  const main = functions.sort((a, b) => b.getEnd() - b.getStart() - (a.getEnd() - a.getStart()))[0];
  const mainName = main?.name?.text;
  if (!main?.body || !mainName) {
    console.log(`SKIP ${relative}: no component`);
    continue;
  }
  const statements = [...main.body.statements];
  const last = statements[statements.length - 1];
  if (!last || !ts.isReturnStatement(last) || statements.length !== 2) {
    console.log(
      `SKIP ${relative}: body is not hook-call + return (${statements.length} statements)`,
    );
    continue;
  }
  const hookStatement = statements[0];
  if (!ts.isVariableStatement(hookStatement)) {
    console.log(`SKIP ${relative}: first statement is not the hook call`);
    continue;
  }
  const hookDeclaration = hookStatement.declarationList.declarations[0];
  const hookCall = hookDeclaration.initializer;
  if (
    !ts.isCallExpression(hookCall) ||
    !ts.isIdentifier(hookCall.expression) ||
    !ts.isObjectBindingPattern(hookDeclaration.name)
  ) {
    console.log(`SKIP ${relative}: hook call not recognised`);
    continue;
  }
  const hookName = hookCall.expression.text;
  const hookArgs = hookCall.arguments.map((argument) => argument.getText(source)).join(", ");
  const names = hookDeclaration.name.elements
    .map((element) => (ts.isBindingElement(element) ? element.name.getText(source) : null))
    .filter(Boolean);
  const jsx = last
    .getText(source)
    .replace(/^return\s*/, "")
    .replace(/;\s*$/, "");

  // Component props become view props as well (the JSX may use them directly, not only through the hook result).
  const propNames = [];
  for (const parameter of main.parameters) bindingNames(parameter.name, propNames);
  const singleParameter = main.parameters.length === 1 ? main.parameters[0] : null;
  const propType = singleParameter?.type ? singleParameter.type.getText(source) : null;
  const propsArg = propNames.length > 0 ? `{ ${propNames.join(", ")} }` : null;
  const propsSignature =
    propsArg && propType
      ? `{ view, ${propNames.join(", ")} }: { view: ReturnType<typeof ${hookName}> } & ${propType}`
      : null;
  const forwardProps = propsArg ? ` {...${propsArg}}` : "";

  const renderImports = (targetText, targetFile) => {
    const lines = [];
    for (const declaration of imports) {
      const clause = declaration.importClause;
      if (!clause) continue;
      const parts = [];
      if (clause.name && refs(targetText, clause.name.text)) parts.push(clause.name.text);
      if (clause.namedBindings) {
        if (ts.isNamespaceImport(clause.namedBindings)) {
          if (refs(targetText, clause.namedBindings.name.text))
            parts.push(`* as ${clause.namedBindings.name.text}`);
        } else {
          const kept = clause.namedBindings.elements
            .filter((element) => refs(targetText, (element.name ?? element.propertyName).text))
            .map((element) => element.getText(source));
          if (kept.length > 0) parts.push(`{ ${kept.join(", ")} }`);
        }
      }
      if (parts.length === 0) continue;
      let specifier = declaration.moduleSpecifier.getText(source).replace(/^["']|["']$/g, "");
      if (specifier.startsWith(".")) {
        const target = path.resolve(baseDir, specifier);
        const relativePath = path
          .relative(path.dirname(targetFile), target)
          .split(path.sep)
          .join("/");
        specifier = relativePath.startsWith(".") ? relativePath : `./${relativePath}`;
      }
      lines.push(
        `import ${clause.isTypeOnly ? "type " : ""}${parts.join(", ")} from "${specifier}";`,
      );
    }
    return lines;
  };

  // The hook lives next to the page, or in the sibling directory the splitter created for it.
  const searchDirs = [
    baseDir,
    ...readdirSync(baseDir, { withFileTypes: true })
      .filter((entry) => entry.isDirectory())
      .map((entry) => path.join(baseDir, entry.name)),
  ];
  let hookPath = null;
  for (const directory of searchDirs) {
    for (const extension of [".tsx", ".ts"]) {
      const candidate = path.join(directory, `${hookName}${extension}`);
      if (existsSync(candidate)) {
        hookPath = candidate;
        break;
      }
    }
    if (hookPath) break;
  }
  const hookModule = hookPath
    ? `./${path
        .relative(baseDir, hookPath)
        .split(path.sep)
        .join("/")
        .replace(/\.tsx?$/, "")}`
    : `./${hookName}`;
  const viewFile = path.join(baseDir, `${mainName}View.tsx`);
  const dialogsFile = path.join(baseDir, `${mainName}ViewDialogs.tsx`);

  // Optional: pull dialog blocks out of the JSX so the view stays under the cap. Blocks are copied verbatim.
  let viewJsx = jsx;
  let dialogsBody = null;
  const dialogs = [];
  for (const pattern of [
    /<Dialog[\s\S]*?<\/Dialog>/g,
    /<ConfirmTypeDialog[\s\S]*?\/>/g,
    /<ConfirmDialog[\s\S]*?\/>/g,
  ]) {
    for (const match of viewJsx.matchAll(pattern)) dialogs.push(match[0]);
  }
  if (dialogs.length > 0) {
    dialogsBody = dialogs.join("\n\n");
    for (const dialog of dialogs) viewJsx = viewJsx.replace(dialog, "");
    viewJsx = viewJsx.replace(
      /<\/PageStack>/,
      `<${mainName}ViewDialogs view={view}${forwardProps} />\n    </PageStack>`,
    );
  }

  const viewSignature = propsSignature ?? `{ view }: { view: ReturnType<typeof ${hookName}> }`;
  writeFileSync(
    viewFile,
    [
      `// View of ${mainName}: the JSX, with the hook result passed in as \`view\`.`,
      "",
      ...renderImports(`${jsx} ${propType ?? ""}`, viewFile),
      `import type { ${hookName} } from "${hookModule}";`,
      ...(dialogsBody
        ? [`import { ${mainName}ViewDialogs } from "./${mainName}ViewDialogs";`]
        : []),
      "",
      `export function ${mainName}View(${viewSignature}) {`,
      `  const { ${names.join(", ")} } = view;`,
      "",
      `  return ${viewJsx};`,
      "}",
      "",
    ].join("\n"),
    "utf8",
  );

  if (dialogsBody) {
    writeFileSync(
      dialogsFile,
      [
        `// Dialogs of ${mainName}, extracted to keep the view under the 500-line cap.`,
        "",
        ...renderImports(`${dialogsBody} ${propType ?? ""}`, dialogsFile),
        `import type { ${hookName} } from "${hookModule}";`,
        "",
        `export function ${mainName}ViewDialogs(${viewSignature}) {`,
        `  const { ${names.join(", ")} } = view;`,
        "",
        "  return (",
        "    <>",
        dialogsBody,
        "    </>",
        "  );",
        "}",
        "",
      ].join("\n"),
      "utf8",
    );
  }

  const mainHead = text.slice(main.getStart(source), main.body.getStart(source)).trimEnd();
  const newMain = [
    `${mainHead} {`,
    `  const view = ${hookName}(${hookArgs});`,
    "",
    `  return <${mainName}View view={view}${forwardProps} />;`,
    "}",
  ].join("\n");
  writeFileSync(
    sourceFile,
    [
      ...renderImports(`${mainHead} ${forwardProps}`, sourceFile),
      `import { ${mainName}View } from "./${path.basename(viewFile).replace(/\.tsx$/, "")}";`,
      `import { ${hookName} } from "${hookModule}";`,
      "",
      newMain,
      "",
    ].join("\n"),
    "utf8",
  );

  const viewLines = readFileSync(viewFile, "utf8").split("\n").length;
  const dialogsLines = dialogsBody ? readFileSync(dialogsFile, "utf8").split("\n").length : 0;
  console.log(
    `${viewLines > 500 ? "OVER " : "ok   "} ${relative}: view ${viewLines}${dialogsBody ? `, dialogs ${dialogsLines}` : ""}`,
  );
}
