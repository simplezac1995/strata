#!/usr/bin/env node
// Keep the supported Java baseline aligned across compilation, CI, and both Docker build paths.

import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const read = (relative) => readFileSync(join(root, relative), 'utf8')

const failures = []
let checks = 0

function check(name, condition) {
  checks += 1
  if (!condition) failures.push(name)
}

const pom = read('strata/pom.xml')
check('Maven java.version is 25', /<java\.version>\s*25\s*<\/java\.version>/.test(pom))
check(
  'Maven compiler release is 25',
  /<maven\.compiler\.release>\s*25\s*<\/maven\.compiler\.release>/.test(pom),
)

const workflow = read('.github/workflows/ci.yml')
const workflowJavaVersions = workflow
  .split(/\r?\n/)
  .filter((line) => line.includes('java-version:'))
  .map((line) => line.slice(line.indexOf('java-version:') + 'java-version:'.length).trim())
const matrixJavaVersions = [...workflow.matchAll(/^\s*java:\s*\[([^\]]+)\]/gm)].flatMap(([, values]) =>
  values
    .split(',')
    .map((value) => value.trim().replace(/^['"]|['"]$/g, '')),
)
check(
  'CI setup-java inputs resolve only to Java 25',
  workflowJavaVersions.length > 0 &&
    workflowJavaVersions.every(
      (value) => value === '"25"' || value === "'25'" || value.includes('matrix.java'),
    ) &&
    matrixJavaVersions.length > 0 &&
    matrixJavaVersions.every((version) => version === '25'),
)

for (const path of ['Dockerfile', 'strata/Dockerfile']) {
  const dockerfile = read(path)
  check(`${path} builds with JDK 25`, /^FROM maven:[^\n]*eclipse-temurin-25\b/m.test(dockerfile))
  check(`${path} runs with JRE 25`, /^FROM eclipse-temurin:25-jre-/m.test(dockerfile))
}

if (failures.length > 0) {
  console.error(`java-runtime:verify failed — ${failures.length} of ${checks} checks`)
  for (const failure of failures) console.error(`  ✗ ${failure}`)
  process.exit(1)
}

console.log(`java-runtime:verify ok — ${checks} Java 25 toolchain declarations agree`)
