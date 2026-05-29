#!/usr/bin/env node

import { existsSync, readdirSync, statSync } from 'node:fs'
import { join, resolve, sep } from 'node:path'

const root = process.cwd()

const frontendSrcAllowedEntries = new Set([
  'apis',
  'components',
  'index.css',
  'main.tsx',
  'objects',
  'pages',
  'router.tsx',
  'system',
  'test',
])
const frontendComponentAllowedLayers = new Set(['ui'])
const frontendSystemAllowedLayers = new Set(['api', 'format', 'i18n'])
const frontendTestAllowedLayers = new Set(['objects', 'system'])
const frontendTestAllowedRootFiles = new Set(['setup.ts'])
const frontendSharedPageLayers = new Set(['components', 'hooks', 'objects', 'routing', 'stores'])

const backendMainScalaAllowedEntries = new Set([
  'Main.scala',
  'database',
  'domains',
  'routes',
  'server',
  'shared',
])
const backendMainDatabaseAllowedLayers = new Set(['table', 'utils'])
const backendMainDomainAllowedLayers = new Set(['api', 'objects', 'routes', 'table', 'utils'])
const backendMainServerAllowedLayers = new Set(['health'])
const backendMainServerAllowedRootFiles = new Set(['ApplicationResources.scala'])
const backendMainSharedAllowedLayers = new Set(['api', 'application', 'objects'])
const backendTestScalaAllowedEntries = new Set(['domains', 'shared'])
const backendTestDomainAllowedLayers = new Set(['api', 'objects', 'table', 'utils'])
const backendTestSharedAllowedLayers = new Set(['api', 'objects'])

function normalizePath(path) {
  return path.split(sep).join('/')
}

function walk(relativePath) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    return []
  }

  return readdirSync(absolutePath)
    .flatMap((entry) => {
      const child = join(relativePath, entry)
      const childAbsolute = resolve(root, child)
      if (statSync(childAbsolute).isDirectory()) {
        return walk(child)
      }
      return [normalizePath(child)]
    })
    .sort()
}

function isKnownPageRoot(entry) {
  return frontendSharedPageLayers.has(entry) || /^[A-Z][A-Za-z0-9]*Page$/.test(entry)
}

function requireNestedLayer(filePath, segments, layerIndex, allowedLayers, label, errors) {
  const layer = segments[layerIndex]
  if (!layer || !allowedLayers.has(layer)) {
    errors.push(`${filePath} is in unknown ${label} layer "${layer ?? '<root>'}"`)
  }
}

function checkFrontendFile(filePath, errors) {
  const segments = filePath.split('/')
  const entry = segments[2]

  if (!frontendSrcAllowedEntries.has(entry)) {
    errors.push(`${filePath} is in unknown frontend src layer "${entry}"`)
    return
  }

  if (entry === 'apis') {
    if (!segments[3] || segments[3].includes('.')) {
      errors.push(`${filePath} is in unknown frontend api domain layer "${segments[3] ?? '<root>'}"`)
    }
    return
  }

  if (entry === 'components') {
    requireNestedLayer(filePath, segments, 3, frontendComponentAllowedLayers, 'frontend components', errors)
    return
  }

  if (entry === 'objects') {
    if (!segments[3] || segments[3].includes('.')) {
      errors.push(`${filePath} is in unknown frontend object domain layer "${segments[3] ?? '<root>'}"`)
    }
    return
  }

  if (entry === 'pages') {
    const pageRoot = segments[3]
    if (!pageRoot || !isKnownPageRoot(pageRoot)) {
      errors.push(`${filePath} is in unknown frontend pages layer "${pageRoot ?? '<root>'}"`)
    }
    return
  }

  if (entry === 'system') {
    requireNestedLayer(filePath, segments, 3, frontendSystemAllowedLayers, 'frontend system', errors)
    return
  }

  if (entry === 'test') {
    if (segments.length === 4 && frontendTestAllowedRootFiles.has(segments[3])) {
      return
    }
    requireNestedLayer(filePath, segments, 3, frontendTestAllowedLayers, 'frontend test', errors)
  }
}

function checkBackendMainFile(filePath, errors) {
  const segments = filePath.split('/')
  const entry = segments[4]

  if (!backendMainScalaAllowedEntries.has(entry)) {
    errors.push(`${filePath} is in unknown backend main scala layer "${entry}"`)
    return
  }

  if (entry === 'database') {
    if (segments.length > 6) {
      requireNestedLayer(filePath, segments, 5, backendMainDatabaseAllowedLayers, 'backend database', errors)
    }
    return
  }

  if (entry === 'domains') {
    const domain = segments[5]
    const layer = segments[6]
    if (!domain || !layer || !backendMainDomainAllowedLayers.has(layer)) {
      errors.push(`${filePath} is in unknown backend domain layer "${layer ?? '<root>'}"`)
    }
    return
  }

  if (entry === 'routes') {
    if (segments.length > 6) {
      errors.push(`${filePath} is in unknown backend routes layer "${segments[5]}"`)
    }
    return
  }

  if (entry === 'server') {
    if (segments.length === 6 && backendMainServerAllowedRootFiles.has(segments[5])) {
      return
    }
    requireNestedLayer(filePath, segments, 5, backendMainServerAllowedLayers, 'backend server', errors)
    return
  }

  if (entry === 'shared') {
    requireNestedLayer(filePath, segments, 5, backendMainSharedAllowedLayers, 'backend shared', errors)
  }
}

function checkBackendTestFile(filePath, errors) {
  const segments = filePath.split('/')
  const entry = segments[4]

  if (!backendTestScalaAllowedEntries.has(entry)) {
    errors.push(`${filePath} is in unknown backend test scala layer "${entry}"`)
    return
  }

  if (entry === 'domains') {
    const domain = segments[5]
    const layer = segments[6]
    if (!domain || !layer || !backendTestDomainAllowedLayers.has(layer)) {
      errors.push(`${filePath} is in unknown backend test domain layer "${layer ?? '<root>'}"`)
    }
    return
  }

  if (entry === 'shared') {
    requireNestedLayer(filePath, segments, 5, backendTestSharedAllowedLayers, 'backend test shared', errors)
  }
}

function run() {
  const errors = []

  for (const filePath of walk('frontend/src')) {
    checkFrontendFile(filePath, errors)
  }

  for (const filePath of walk('backend/src/main/scala')) {
    checkBackendMainFile(filePath, errors)
  }

  for (const filePath of walk('backend/src/test/scala')) {
    checkBackendTestFile(filePath, errors)
  }

  if (errors.length > 0) {
    console.error(`Known layer check failed with ${errors.length} issue(s):`)
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exit(1)
  }

  console.log('Known layer check passed.')
}

run()
