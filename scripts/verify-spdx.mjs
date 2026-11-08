// Verify every frontend TypeScript source starts with the Apache-2.0 SPDX identifier.
// Scans the directories that exist in the current checkout, so the same script works in the backend repo
// (strata-ui only) and in the published monorepo (strata-ui + frontend).
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const ROOT = process.cwd()
const ROOTS = ['strata-ui/src', 'frontend/src'].filter((dir) => {
  try {
    return statSync(join(ROOT, dir)).isDirectory()
  } catch {
    return false
  }
})
if (ROOTS.length === 0) {
  // The open-source framework export is backend-only; there are no frontend sources to check there.
  console.log('SPDX ok: no frontend sources in this checkout (backend-only export)')
  process.exit(0)
}

const missing = []
let scanned = 0
const walk = (dir) => {
  for (const entry of readdirSync(dir)) {
    if (entry === 'node_modules' || entry === 'dist') continue
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) walk(full)
    else if (/\.(ts|tsx)$/.test(entry)) {
      scanned++
      const first = readFileSync(full, 'utf8').split('\n', 1)[0]
      if (!first.includes('SPDX-License-Identifier: Apache-2.0')) missing.push(relative(ROOT, full))
    }
  }
}
for (const dir of ROOTS) walk(join(ROOT, dir))

if (missing.length > 0) {
  console.error(`missing SPDX header in ${missing.length}/${scanned} files:`)
  for (const file of missing.slice(0, 30)) console.error(`  ${file}`)
  process.exit(1)
}
console.log(`SPDX ok: ${scanned} files in ${ROOTS.join(', ')}`)
