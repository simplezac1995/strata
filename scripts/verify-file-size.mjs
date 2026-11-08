#!/usr/bin/env node
// Enforce the 500-line source-file cap documented in docs/17-结构约定.md (section "超大文件").
//
// The cap only holds if something checks it: the split tooling (strata-ui/scripts/auto-split.mjs,
// extract-view.mjs) knows how to shrink a file, this gate refuses to let one grow back past the line.
//
// Language dictionaries are data, not code: i18n directories are excluded on purpose (a 3,000-key dictionary is
// allowed to be long; the checks are for hand-written modules).
import { readFileSync, readdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join, relative } from 'node:path'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const MAX_LINES = 500
const SKIP_DIRECTORIES = new Set(['node_modules', 'target', 'dist', '.git', 'build', 'coverage', 'test-results'])

const targets = [
  { directory: 'strata', extensions: ['.java'], exclude: [] },
  { directory: 'strata-ui/src', extensions: ['.ts', '.tsx'], exclude: ['/i18n/'] },
  { directory: 'frontend/src', extensions: ['.ts', '.tsx'], exclude: ['/i18n/'] },
]

const offenders = []
let checked = 0
let largest = { lines: 0, file: '' }

function walk(directory, extensions, exclude) {
  let entries
  try {
    entries = readdirSync(directory, { withFileTypes: true })
  } catch {
    return
  }
  for (const entry of entries) {
    if (SKIP_DIRECTORIES.has(entry.name)) continue
    const full = join(directory, entry.name)
    if (entry.isDirectory()) {
      walk(full, extensions, exclude)
      continue
    }
    if (!extensions.some((extension) => entry.name.endsWith(extension))) continue
    const relativePath = relative(root, full).split('\\').join('/')
    if (exclude.some((marker) => `/${relativePath}`.includes(marker))) continue
    const lines = readFileSync(full, 'utf8').split(/\r?\n/).length
    checked += 1
    if (lines > largest.lines) largest = { lines, file: relativePath }
    if (lines > MAX_LINES) offenders.push({ lines, file: relativePath })
  }
}

for (const target of targets) {
  walk(join(root, target.directory), target.extensions, target.exclude)
}

if (offenders.length > 0) {
  offenders.sort((a, b) => b.lines - a.lines)
  console.error(`file-size: ${offenders.length} file(s) over ${MAX_LINES} lines:`)
  for (const offender of offenders) console.error(`  ${String(offender.lines).padStart(5)}  ${offender.file}`)
  console.error('Split them (docs/17 §六) instead of raising the limit.')
  process.exit(1)
}

console.log(`file-size: ${checked} files checked, largest ${largest.lines} lines (${largest.file}) — all <= ${MAX_LINES}`)
