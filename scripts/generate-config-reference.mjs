// Generate docs/配置参考.md from the Spring Boot configuration metadata that the build already produces.
// Each module's spring-boot-configuration-processor writes target/classes/META-INF/spring-configuration-metadata.json;
// this script aggregates them, so the reference cannot drift from the @ConfigurationProperties code.
//
// Usage:
//   node scripts/generate-config-reference.mjs          write the doc
//   node scripts/generate-config-reference.mjs --check  fail if the committed doc is stale
import { readFileSync, readdirSync, statSync, writeFileSync, existsSync } from 'node:fs'
import { join, relative, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const OUTPUT = join(ROOT, 'docs', '配置参考.md')
const CHECK = process.argv.includes('--check')

const metadataFiles = []
const walk = (dir) => {
  let entries
  try {
    entries = readdirSync(dir)
  } catch {
    return
  }
  for (const entry of entries) {
    if (entry === 'node_modules' || entry === '.git') continue
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) walk(full)
    else if (entry === 'spring-configuration-metadata.json') metadataFiles.push(full)
  }
}
walk(join(ROOT, 'strata'))

if (metadataFiles.length === 0) {
  console.error('no spring-configuration-metadata.json found; run `mvn -DskipTests compile` first')
  process.exit(2)
}

/** @type {Map<string, {name: string, type?: string, defaultValue?: unknown, description?: string, deprecated?: string}>} */
const properties = new Map()
for (const file of metadataFiles) {
  let metadata
  try {
    metadata = JSON.parse(readFileSync(file, 'utf8'))
  } catch (error) {
    console.error(`skipping unreadable metadata ${relative(ROOT, file)}: ${error.message}`)
    continue
  }
  for (const property of metadata.properties ?? []) {
    const existing = properties.get(property.name)
    // app-level metadata (merged, with defaults resolved) wins over per-module metadata
    if (existing && existing.__merged) continue
    properties.set(property.name, { ...property, __merged: existing?.__merged ?? property.name.startsWith('strata.') })
  }
}

const rows = [...properties.values()].filter((property) => !property.deprecated).sort((a, b) => a.name.localeCompare(b.name))
if (rows.length === 0) {
  console.error('configuration metadata contained no properties')
  process.exit(2)
}

const escape = (value) => String(value).replaceAll('|', '\\|')
const formatType = (type) =>
  (type ?? '')
    .replace('java.lang.String', 'String')
    .replace('java.lang.Integer', 'Integer')
    .replace('java.lang.Long', 'Long')
    .replace('java.lang.Boolean', 'Boolean')
    .replace('java.util.List<', 'List<')
    .replace('java.time.Duration', 'Duration')
    .replace('java.nio.charset.Charset', 'Charset')

const groups = new Map()
for (const row of rows) {
  const prefix = row.name.split('.')[0] === 'strata' ? row.name.split('.').slice(0, 2).join('.') : row.name.split('.').slice(0, 2).join('.')
  if (!groups.has(prefix)) groups.set(prefix, [])
  groups.get(prefix).push(row)
}

let out = `# 配置参考

> 本页由 \`scripts/generate-config-reference.mjs\` 从各模块编译产物
> \`META-INF/spring-configuration-metadata.json\` 自动生成，**不要手改**。
> 修改配置项：改对应 \`@ConfigurationProperties\` 类后重新生成；CI 会校验本页与代码一致。
>
> 前置：\`cd strata && mvn -DskipTests compile\`（离线用 \`-o\`）。
>
> 环境变量覆盖：Spring Boot 的 relaxed binding 规则，例如 \`strata.security.jwt.secret\` →
> \`STRATA_SECURITY_JWT_SECRET\`。生产环境请用环境变量而不是改 yml。

共 **${rows.length}** 个配置项，按前缀分组。

`
for (const [prefix, groupRows] of [...groups.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
  out += `## ${prefix} (${groupRows.length})\n\n`
  out += '| 属性 | 类型 | 默认值 | 说明 |\n| :--- | :--- | :--- | :--- |\n'
  for (const row of groupRows) {
    const defaultValue = row.defaultValue === undefined ? '' : `\`${escape(JSON.stringify(row.defaultValue))}\``
    out += `| \`${row.name}\` | ${escape(formatType(row.type))} | ${defaultValue} | ${escape(row.description ?? '')} |\n`
  }
  out += '\n'
}

if (CHECK) {
  if (!existsSync(OUTPUT)) {
    console.error(`missing ${relative(ROOT, OUTPUT)}; run the generator`)
    process.exit(1)
  }
  const committed = readFileSync(OUTPUT, 'utf8')
  if (committed !== out) {
    console.error(`${relative(ROOT, OUTPUT)} is stale; run \`node scripts/generate-config-reference.mjs\``)
    process.exit(1)
  }
  console.log(`config reference up to date (${rows.length} properties)`)
  process.exit(0)
}

writeFileSync(OUTPUT, out, 'utf8')
console.log(`wrote ${relative(ROOT, OUTPUT)} (${rows.length} properties from ${metadataFiles.length} metadata files)`)
