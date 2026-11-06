#!/usr/bin/env node
// Guard for the open-source export: it must never contain private infrastructure, credentials or business data.
//
// Run from the export root (CI) or against a directory: node scripts/verify-no-private-data.mjs [root]
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const ROOT = process.argv[2] ?? process.cwd()
const SKIP = new Set(['.git', 'node_modules', 'target', 'dist'])
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
