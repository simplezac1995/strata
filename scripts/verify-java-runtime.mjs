#!/usr/bin/env node
// Keep the Java baseline aligned across compilation, CI and the container images.
//
//   build toolchain : JDK 25 (one matrix, no second toolchain to maintain)
//   bytecode target : Java 21 (maven.compiler.release=21, no preview features)
//   runtime floor   : Java 21 (CI starts the built application on a Java 21 JRE)

import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const read = (relative) => readFileSync(join(root, relative), 'utf8')

const failures = []
let checks = 0
const check = (name, condition) => {
  checks += 1
  if (!condition) failures.push(name)
}

const pom = read('strata/pom.xml')
check('Maven java.version is 21 (bytecode target)', /<java\.version>\s*21\s*<\/java\.version>/.test(pom))
check(
  'Maven compiler release is 21 (bytecode target)',
  /<maven\.compiler\.release>\s*21\s*<\/maven\.compiler\.release>/.test(pom),
)

const workflow = read('.github/workflows/ci.yml')
check('CI has a Java 21 runtime job', /name:\s*Runtime\s*·\s*Java 21/.test(workflow))
check('CI runtime job sets up Java 21', /java-version:\s*["']21["']/.test(workflow))
check('CI build jobs keep the JDK 25 toolchain', /java-version:\s*["']25["']/.test(workflow))
const buildMatrix = /^\s*java:\s*\[\"?25\"?\]\s*$/m.test(workflow)
check('CI build matrix is JDK 25 only', buildMatrix)

for (const path of ['Dockerfile', 'strata/Dockerfile']) {
  const dockerfile = read(path)
  check(`${path} builds with the JDK 25 toolchain (maven image)`, /^FROM maven:[^\n]*eclipse-temurin-25\b/m.test(dockerfile))
  check(`${path} runs on a Java 21 JRE`, /^FROM eclipse-temurin:21-jre-/m.test(dockerfile))
}

if (failures.length > 0) {
  console.error(`java-runtime:verify failed — ${failures.length} of ${checks} checks`)
  for (const failure of failures) console.error(`  - ${failure}`)
  process.exit(1)
}

console.log(`java-runtime:verify ok — ${checks} checks: build JDK 25, bytecode Java 21, runtime Java 21`)
