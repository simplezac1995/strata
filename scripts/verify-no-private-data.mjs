#!/usr/bin/env node
// Guard for the open-source export: it must never contain private infrastructure, credentials or business data.
//
// Run from the export root (CI) or against a directory: node scripts/verify-no-private-data.mjs [root]
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const ROOT = process.argv[2] ?? process.cwd()
const SKIP = new Set(['.git', 'node_modules', 'target', 'dist'])
// The guard itself contains every forbidden literal (pattern definitions and examples) by definition; only its path is
// checked, its content is exempt, otherwise the scan would always report itself.
const GUARD_FILE = 'scripts/verify-no-private-data.mjs'

// ------------------------------------------------------------------ secrets / private infrastructure
const PATTERNS = [
  { name: 'production IP', regex: /\b47\.245\.15\.149\b/ },
  { name: 'production domain', regex: /simplecharlie\.dev/i },
  { name: 'admin production host', regex: /gadmin\./i },
  { name: 'private business product', regex: /\bbobole\b/i },
  { name: 'GitHub token', regex: /\bghp_[A-Za-z0-9]{30,}\b/ },
  { name: 'fine-grained GitHub token', regex: /\bgithub_pat_[A-Za-z0-9_]{20,}\b/ },
  { name: 'AWS access key', regex: /\bAKIA[0-9A-Z]{16}\b/ },
  { name: 'private key block', regex: /-----BEGIN (?:RSA |OPENSSH |EC |PGP )?PRIVATE KEY-----/ },
  { name: 'developer home path', regex: /[A-Za-z]:\\Users\\[A-Za-z]+/ },
  { name: 'private repository path', regex: /E:\\Projects\\/i },
  { name: 'private key literal', regex: /STRATA_ENCRYPT[_A-Z]*KEY=[^$\s][^\s]{16,}/ },
]

// ------------------------------------------------------------------ business residue
// The open-source repository is the framework only. The commercial verticals (casino/pro/enterprise), their
// deployable app and the business half of the admin console stay private. Path checks catch whole directories that
// slipped through the export, content checks catch references that survived a partial pruning.
const BUSINESS_PATH_PATTERNS = [
  { name: 'private Maven module', regex: /^strata\/strata-(?:casino|pro|enterprise|app|migrations|coverage)(?:\/|$)/ },
  {
    name: 'business admin UI page',
    regex: /^strata-ui\/src\/pages\/(?:casino|compliance|legal|marketing|member|player|pro|risk|finance|support)(?:\/|$)/,
  },
  {
    name: 'business admin UI api client',
    regex: /^strata-ui\/src\/shared\/api\/(?:casino|compliance|marketing|member|player|point|pro|rebate|reconciliation|risk|stats|ticket)(?:\.tsx?|\/)/,
  },
  { name: 'business admin UI navigation', regex: /^strata-ui\/src\/components\/shell\/nav\/(?:business|casino)\.ts$/ },
  {
    name: 'business admin UI dictionary',
    regex: /^strata-ui\/src\/shared\/i18n\/dict\/[a-z]{2}\/(?:casino|compliance|legal|marketing|member|player|pro|risk)\.ts$/,
  },
  { name: 'private product documentation', regex: /^docs\/(?:bobole-|[0-9]{2}-)/ },
  { name: 'private product frontend', regex: /^frontend\// },
  { name: 'legacy Chinese document filename', regex: /^docs\/配置参考\.md$/ },
]

// Business words are only forbidden where they identify the private product's console or its Maven coordinates.
// Framework code may legitimately talk about domain-shaped extension points, and a couple of files use a casino
// authority as the generic "sensitive surface" example; those files are whitelisted explicitly.
const BUSINESS_CONTENT_PATTERNS = [
  {
    name: 'business admin UI page import',
    regex: /["']@\/pages\/(?:casino|compliance|legal|marketing|member|player|pro|risk|finance|support)\//,
    scope: /^strata-ui\//,
  },
  {
    name: 'business admin UI api import',
    regex: /["']@\/shared\/api\/(?:casino|compliance|marketing|member|player|point|pro|rebate|reconciliation|risk|stats|ticket)["'/]/,
    scope: /^strata-ui\//,
  },
  { name: 'business admin UI route', regex: /["'`]\/(?:casino|compliance)\//, scope: /^strata-ui\// },
  {
    name: 'business admin UI navigation key',
    regex: /"nav\.(?:casino|compliance|player|member|finance|marketing|risk)\b/,
    scope: /^strata-ui\//,
  },
  {
    name: 'business admin UI page name',
    regex:
      /\b(?:ResponsibleGambling|KycReview|AmlDashboard|GeoRestrictions|ChannelLimits|ExchangeRates|CasinoDashboard|WalletTron(?:Hot|Cold|History|Review)|WalletBsc(?:Hot|Cold|History|Review))\b/,
    scope: /^strata-ui\//,
  },
  {
    name: 'casino wallet authority',
    regex: /casino:(?:wallet|player|game|bonus|vip|affiliate|dashboard|risk|fee|recon|payout|rpc|leaderboard|asynctask)/,
    scope: /^strata-ui\//,
    // The generic money-surface guard and its test intentionally use the casino wallet prefix as an example.
    allow: [/^strata-ui\/src\/shared\/auth\/store(?:\.test)?\.ts$/, /^strata-ui\/src\/routes\/guards\.tsx$/],
  },
  {
    name: 'business Maven artifact',
    regex: /<artifactId>strata-(?:casino|pro|enterprise|app|migrations|coverage)[^<]*<\/artifactId>/,
    scope: /^strata\/(?:[^/]+\/)*pom\.xml$/,
  },
  {
    name: 'gambling business word in the admin console',
    regex: /\b(?:casino|gambling|kyc|aml|wallet)\b/i,
    scope: /^strata-ui\/(?:src|e2e)\//,
    allow: [/^strata-ui\/src\/shared\/auth\/store(?:\.test)?\.ts$/, /^strata-ui\/src\/routes\/guards\.tsx$/],
  },
]

const files = []
const walk = (dir) => {
  for (const entry of readdirSync(dir)) {
    if (SKIP.has(entry)) continue
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) walk(full)
    else files.push(full)
  }
}
walk(ROOT)

const findings = []
for (const file of files) {
  const rel = relative(ROOT, file).split('\\').join('/')
  for (const pattern of BUSINESS_PATH_PATTERNS) {
    if (pattern.regex.test(rel)) findings.push({ file: rel, pattern: pattern.name, sample: rel })
  }
  if (rel === GUARD_FILE) continue // the guard's own literals are not private data (see GUARD_FILE)
  // binary-ish files are not interesting for these patterns
  if (/\.(png|jpg|jpeg|gif|webp|ico|woff2?|ttf|eot|jar|zip|class|bpmn20|drawio)$/i.test(entry(file))) continue
  let text
  try {
    text = readFileSync(file, 'utf8')
  } catch {
    continue
  }
  for (const pattern of PATTERNS) {
    const match = pattern.regex.exec(text)
    if (match) findings.push({ file: rel, pattern: pattern.name, sample: lineOf(text, match.index) })
  }
  for (const pattern of BUSINESS_CONTENT_PATTERNS) {
    if (pattern.scope && !pattern.scope.test(rel)) continue
    if (pattern.allow && pattern.allow.some((allow) => allow.test(rel))) continue
    const match = pattern.regex.exec(text)
    if (match) findings.push({ file: rel, pattern: pattern.name, sample: lineOf(text, match.index) })
  }
}

function entry(file) {
  return file.split(/[\\/]/).pop()
}

function lineOf(text, index) {
  const start = text.lastIndexOf('\n', index) + 1
  const end = text.indexOf('\n', index)
  return text.slice(start, end === -1 ? undefined : end).trim().slice(0, 120)
}

if (findings.length === 0) {
  console.log(`private-data scan ok: ${files.length} files checked`)
  process.exit(0)
}
console.error(`private-data scan found ${findings.length} finding(s):`)
for (const finding of findings.slice(0, 40)) console.error(`  [${finding.pattern}] ${finding.file}: ${finding.sample}`)
process.exit(1)
