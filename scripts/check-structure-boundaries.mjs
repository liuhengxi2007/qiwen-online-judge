import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { dirname, extname, join, relative, resolve, sep } from 'node:path'

const root = process.cwd()

const frontendBlockedModelSegments = new Set(['http', 'hooks', 'components', 'pages'])
const frontendBlockedDomainSegments = new Set(['hooks', 'components', 'pages'])
const backendBlockedModelSegments = new Set(['application', 'http', 'table'])
const backendApplicationWireCodecImportPattern = /^io\.circe(?:\.|$|\{)/
const backendWireCodecImportPattern = /^io\.circe(?:\.|$|\{)/
const backendModelPersistenceHelperPattern = /\bdef\s+(?:toDatabase|fromDatabase)\b/g

const pathOf = (...parts) => parts.join('/')
const extension = (name, ext) => `${name}.${ext}`

const bannedTrackedPaths = new Set([
  pathOf('backend', extension('package-lock', 'json')),
  pathOf('frontend', 'src', 'assets', extension('hero', 'png')),
  pathOf('frontend', 'src', 'assets', extension('react', 'svg')),
  pathOf('frontend', 'src', 'assets', extension('vite', 'svg')),
  pathOf('frontend', 'src', 'stores', 'use-app-store.ts'),
])

function normalizePath(path) {
  return path.split(sep).join('/')
}

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
}

function walk(relativePath, extensions) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    return []
  }

  return readdirSync(absolutePath)
    .flatMap((entry) => {
      const child = join(relativePath, entry)
      const childAbsolute = resolve(root, child)
      if (statSync(childAbsolute).isDirectory()) {
        return walk(child, extensions)
      }
      return extensions.has(extname(child)) ? [normalizePath(child)] : []
    })
    .sort()
}

function lineNumber(source, index) {
  return source.slice(0, index).split('\n').length
}

function basenameWithoutExtension(filePath) {
  const basename = filePath.split('/').at(-1) ?? filePath
  return basename.replace(/\.[^.]+$/, '')
}

function resolveFrontendSpecifier(filePath, specifier) {
  if (specifier.startsWith('@/')) {
    return `frontend/src/${specifier.slice(2)}`
  }

  if (specifier.startsWith('.')) {
    return normalizePath(relative(root, resolve(root, dirname(filePath), specifier)))
  }

  return specifier
}

function hasBlockedSegment(path, blockedSegments) {
  return path.split('/').some((segment) => blockedSegments.has(segment))
}

function extractTsSpecifiers(source) {
  const importPattern =
    /\b(?:import|export)\s+(?:type\s+)?(?:[^'"]*?\s+from\s+)?['"]([^'"]+)['"]/g
  return [...source.matchAll(importPattern)].map((match) => ({
    specifier: match[1],
    index: match.index ?? 0,
  }))
}

function checkFrontendFile(filePath, blockedSegments, errors) {
  const source = read(filePath)
  for (const match of extractTsSpecifiers(source)) {
    const resolved = resolveFrontendSpecifier(filePath, match.specifier)
    if (hasBlockedSegment(resolved, blockedSegments)) {
      errors.push(
        `${filePath}:${lineNumber(source, match.index)} imports forbidden frontend layer "${match.specifier}"`,
      )
    }
  }
}

function extractScalaImports(source) {
  return source
    .split('\n')
    .map((line, index) => ({ line: line.trim(), lineNumber: index + 1 }))
    .filter((entry) => entry.line.startsWith('import '))
}

function checkBackendModelFile(filePath, errors) {
  const source = read(filePath)
  for (const entry of extractScalaImports(source)) {
    const importedPath = entry.line.replace(/^import\s+/, '')
    if (hasBlockedSegment(importedPath.replace(/[{}]/g, '.').replace(/,/g, '.'), backendBlockedModelSegments)) {
      errors.push(`${filePath}:${entry.lineNumber} imports forbidden backend layer "${importedPath}"`)
    }
    if (backendWireCodecImportPattern.test(importedPath)) {
      errors.push(`${filePath}:${entry.lineNumber} imports HTTP wire codec package "${importedPath}"`)
    }
  }

  for (const match of source.matchAll(backendModelPersistenceHelperPattern)) {
    errors.push(`${filePath}:${lineNumber(source, match.index ?? 0)} defines persistence helper "${match[0].trim()}"`)
  }
}

function checkBackendApplicationBoundaryFile(filePath, errors) {
  const source = read(filePath)
  for (const entry of extractScalaImports(source)) {
    const importedPath = entry.line.replace(/^import\s+/, '')
    if (backendApplicationWireCodecImportPattern.test(importedPath)) {
      errors.push(`${filePath}:${entry.lineNumber} imports HTTP wire codec package "${importedPath}"`)
    }
  }
}

function extractFrontendTopLevelModelTypes(source) {
  const cleanSource = source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/(^|\s)\/\/.*$/gm, '')

  return [...cleanSource.matchAll(/^export\s+(?:type|interface)\s+([A-Za-z_$][\w$]*)/gm)].map(
    (match) => match[1],
  )
}

function extractBackendTopLevelModelTypes(source) {
  const cleanSource = source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/(^|\s)\/\/.*$/gm, '')

  return [
    ...cleanSource.matchAll(/^(?:final\s+case\s+class|enum|sealed\s+trait|type)\s+([A-Za-z_$][\w$]*)/gm),
  ].map((match) => match[1])
}

function checkOneTopLevelModelType(filePath, types, errors) {
  if (types.length === 0) {
    return
  }

  if (types.length > 1) {
    errors.push(`${filePath} defines multiple top-level model types: ${types.join(', ')}`)
    return
  }

  const expectedName = basenameWithoutExtension(filePath)
  if (types[0] !== expectedName) {
    errors.push(`${filePath} defines ${types[0]}, but model file basename must match the type name`)
  }
}

function checkFrontendModelFileShape(filePath, errors) {
  checkOneTopLevelModelType(filePath, extractFrontendTopLevelModelTypes(read(filePath)), errors)
}

function checkBackendModelFileShape(filePath, errors) {
  checkOneTopLevelModelType(filePath, extractBackendTopLevelModelTypes(read(filePath)), errors)
}

function trackedFiles() {
  const result = spawnSync('git', ['ls-files'], { cwd: root, encoding: 'utf8' })
  if (result.status !== 0 || !result.stdout) {
    return []
  }

  return result.stdout
    .split('\n')
    .map((entry) => entry.trim())
    .filter(Boolean)
}

function checkTrackedResidues(errors) {
  for (const filePath of trackedFiles()) {
    if (!existsSync(resolve(root, filePath))) {
      continue
    }

    const segments = filePath.split('/')
    const basename = segments.at(-1) ?? filePath

    if (bannedTrackedPaths.has(filePath)) {
      errors.push(`${filePath} is a tracked template or misplaced file`)
    }

    if (segments.includes('dist')) {
      errors.push(`${filePath} is tracked generated dist output`)
    }

    if (/_backup\.[^/]+$/.test(basename)) {
      errors.push(`${filePath} is a tracked backup file`)
    }
  }
}

function run() {
  const errors = []

  for (const filePath of walk('frontend/src/features', new Set(['.ts', '.tsx']))) {
    if (/^frontend\/src\/features\/[^/]+\/model\//.test(filePath)) {
      checkFrontendFile(filePath, frontendBlockedModelSegments, errors)
      checkFrontendModelFileShape(filePath, errors)
    }

    if (/^frontend\/src\/features\/[^/]+\/domain\//.test(filePath)) {
      checkFrontendFile(filePath, frontendBlockedDomainSegments, errors)
    }
  }

  for (const filePath of walk('frontend/src/shared/model', new Set(['.ts', '.tsx']))) {
    checkFrontendFile(filePath, frontendBlockedModelSegments, errors)
    checkFrontendModelFileShape(filePath, errors)
  }

  for (const filePath of walk('backend/src/main/scala/domains', new Set(['.scala']))) {
    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/model\//.test(filePath)) {
      checkBackendModelFile(filePath, errors)
      checkBackendModelFileShape(filePath, errors)
    }

    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/application\/(?:input|output)\//.test(filePath)) {
      checkBackendApplicationBoundaryFile(filePath, errors)
    }
  }

  for (const filePath of walk('backend/src/main/scala/shared/model', new Set(['.scala']))) {
    checkBackendModelFile(filePath, errors)
    checkBackendModelFileShape(filePath, errors)
  }

  for (const filePath of walk('backend/src/main/scala/shared/access', new Set(['.scala']))) {
    checkBackendModelFile(filePath, errors)
  }

  checkTrackedResidues(errors)

  if (errors.length > 0) {
    console.error(`Structure boundary check failed with ${errors.length} issue(s):`)
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exit(1)
  }

  console.log('Structure boundary check passed.')
}

run()
