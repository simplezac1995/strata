#!/usr/bin/env node
// Aggregate the per-module JaCoCo CSV reports produced by `mvn verify` into a markdown summary for CI.
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const root = process.cwd()
const files = []
const walk = (dir) => {
  for (const entry of readdirSync(dir)) {
    if (['node_modules', '.git', 'dist'].includes(entry)) continue
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) walk(full)
    else if (entry === 'jacoco.csv') files.push(full)
  }
}
walk(root)

if (files.length === 0) {
  console.log('coverage: no JaCoCo reports found (run `mvn verify` first)')
  process.exit(0)
}

const rows = []
for (const file of files) {
  const lines = readFileSync(file, 'utf8').trim().split('\n').slice(1)
  let lineCovered = 0
  let lineTotal = 0
  let branchCovered = 0
  let branchTotal = 0
  for (const line of lines) {
    const cells = line.split(',')
    branchCovered += Number(cells[5] ?? 0)
    branchTotal += Number(cells[6] ?? 0)
    lineCovered += Number(cells[8] ?? 0)
    lineTotal += Number(cells[9] ?? 0)
  }
  const module = relative(root, file).split('\\').join('/').replace('/target/site/jacoco/jacoco.csv', '')
  rows.push({
    module,
    line: lineTotal === 0 ? 0 : (100 * lineCovered) / lineTotal,
    branch: branchTotal === 0 ? 0 : (100 * branchCovered) / branchTotal,
    lineCovered,
    lineTotal,
  })
}

rows.sort((a, b) => a.line - b.line)
const totalCovered = rows.reduce((sum, row) => sum + row.lineCovered, 0)
const totalLines = rows.reduce((sum, row) => sum + row.lineTotal, 0)
const overall = totalLines === 0 ? 0 : (100 * totalCovered) / totalLines

console.log('## Coverage (JaCoCo line coverage)')
console.log('')
console.log(`**Overall: ${overall.toFixed(1)}%** (${totalCovered}/${totalLines} lines)`)
console.log('')
console.log('| Module | Line | Branch | Lines |')
console.log('| :--- | ---: | ---: | ---: |')
for (const row of rows) {
  console.log(
    `| ${row.module} | ${row.line.toFixed(1)}% | ${row.branch.toFixed(1)}% | ${row.lineCovered}/${row.lineTotal} |`,
  )
}
