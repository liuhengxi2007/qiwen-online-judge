#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import { readdirSync } from 'node:fs'
import { basename, dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptPath = fileURLToPath(import.meta.url)
const scriptDirectory = dirname(scriptPath)
const root = resolve(scriptDirectory, '..')
const currentScript = basename(scriptPath)

const checkScripts = readdirSync(scriptDirectory)
  .filter((name) => /^check-.*\.mjs$/.test(name))
  .filter((name) => name !== currentScript)
  .sort()

if (checkScripts.length === 0) {
  console.log('No check scripts found.')
  process.exit(0)
}

let failed = false

for (const script of checkScripts) {
  const relativeScriptPath = relative(root, join(scriptDirectory, script))
  console.log(`\n==> node ${relativeScriptPath}`)

  const result = spawnSync(process.execPath, [relativeScriptPath], {
    cwd: root,
    stdio: 'inherit',
    env: process.env,
  })

  if (result.error) {
    console.error(`${relativeScriptPath} failed to start: ${result.error.message}`)
    failed = true
    continue
  }

  if (result.status !== 0) {
    failed = true
  }
}

if (failed) {
  console.error('\nOne or more check scripts failed.')
  process.exit(1)
}

console.log('\nAll check scripts passed.')
