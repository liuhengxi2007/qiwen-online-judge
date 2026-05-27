import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { dirname, extname, join, relative, resolve, sep } from 'node:path'

const root = process.cwd()

const frontendObjectForbiddenPrefixes = [
  'frontend/src/apis/',
  'frontend/src/system/',
  'frontend/src/pages/',
  'frontend/src/components/ui/',
]
const frontendSystemForbiddenPrefixes = [
  'frontend/src/apis/',
  'frontend/src/pages/',
  'frontend/src/components/ui/',
]
const frontendApiForbiddenPrefixes = [
  'frontend/src/pages/',
  'frontend/src/components/ui/',
]
const frontendPageStoreForbiddenPrefixes = [
  'frontend/src/apis/',
  'frontend/src/system/',
  'frontend/src/components/ui/',
]
const frontendUiForbiddenPrefixes = [
  'frontend/src/apis/',
  'frontend/src/objects/',
  'frontend/src/pages/',
]
const backendBlockedObjectSegments = new Set(['application', 'http', 'table'])
const backendApplicationWireCodecImportPattern = /^io\.circe(?:\.|$|\{)/
const backendWireCodecImportPattern = /^io\.circe(?:\.|$|\{)/
const backendObjectPersistenceHelperPattern = /\bdef\s+(?:toDatabase|fromDatabase)\b/g
const frontendApiRootCodecPathPattern = /^frontend\/src\/apis\/[^/]+\/[A-Za-z0-9_]*(?:Model)?HttpCodecs\.ts$/
const frontendApiCodecIndexPathPattern = /^frontend\/src\/apis\/[^/]+\/codecs\/index\.ts$/
const frontendApiCodecFilePattern = /^frontend\/src\/apis\/([^/]+)\/codecs\/([^/]+)\.ts$/
const frontendApiCodecBasenamePattern = /^[A-Za-z0-9_]*(?:Model)?HttpCodecs$/
const backendDomainHttpCodecFilePattern =
  /^backend\/src\/main\/scala\/domains\/([^/]+)\/http\/codec\/([^/]+)\.scala$/
const backendDomainHttpMapperFilePattern =
  /^backend\/src\/main\/scala\/domains\/([^/]+)\/http\/mapper\/([^/]+)\.scala$/
const backendDomainHttpMapperBasenamePattern = /^[A-Za-z0-9_]+Http(?:Request|Response)Mappers$/
const backendDomainTableImportPattern = /^domains\.[^.]+\.table(?:\.|$|\{)/
const backendDomainTableReferencePattern = /\b[A-Z][A-Za-z0-9]*Table\b/g
const backendEffectPattern =
  /(?:\.prepareStatement\s*\(|\bFiles\.|\bInstant\.now\s*\(|\bLocalDateTime\.now\s*\(|\bSystem\.currentTimeMillis\s*\(|\bUUID\.randomUUID\s*\(|\bSecureRandom\s*\(|\.nextBytes\s*\(|\.publish1\s*\(|\.publish\s*\(|\bclient\.(?:get|set|setex|del|listObjects|putObject|getObject|removeObject|bucketExists|makeBucket)\s*\(|\bproblemDataStorage\.(?:list|read|write|delete|snapshot|restore)\w*\s*\()/
const backendEffectfulReturnPattern = /:\s*(?:IO|Resource)\s*\[|:\s*Stream\s*\[\s*IO\b/

const pathOf = (...parts) => parts.join('/')
const extension = (name, ext) => `${name}.${ext}`
const removedFrontendFeatureRoot = `${pathOf('frontend', 'src', 'features')}/`
const removedFrontendSharedRoot = `${pathOf('frontend', 'src', 'shared')}/`
const removedFrontendStoreRoot = `${pathOf('frontend', 'src', 'stores')}/`

const bannedTrackedPaths = new Set([
  pathOf('backend', extension('package-lock', 'json')),
  pathOf('frontend', 'src', 'assets', extension('hero', 'png')),
  pathOf('frontend', 'src', 'assets', extension('react', 'svg')),
  pathOf('frontend', 'src', 'assets', extension('vite', 'svg')),
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

function hasPrefix(path, prefixes) {
  return prefixes.some((prefix) => path.startsWith(prefix))
}

function extractTsSpecifiers(source) {
  const importPattern =
    /\b(?:import|export)\s+(?:type\s+)?(?:[^'"]*?\s+from\s+)?['"]([^'"]+)['"]/g
  return [...source.matchAll(importPattern)].map((match) => ({
    specifier: match[1],
    index: match.index ?? 0,
  }))
}

function checkFrontendLayerFile(filePath, errors) {
  const source = read(filePath)
  for (const match of extractTsSpecifiers(source)) {
    const resolved = resolveFrontendSpecifier(filePath, match.specifier)
    const line = lineNumber(source, match.index)

    if (resolved.startsWith('frontend/src/pages/objects/') && !filePath.startsWith('frontend/src/pages/')) {
      errors.push(
        `${filePath}:${line} imports page-only object "${match.specifier}" from outside pages`,
      )
    }

    if (filePath.startsWith('frontend/src/objects/')) {
      if (hasPrefix(resolved, frontendObjectForbiddenPrefixes)) {
        errors.push(`${filePath}:${line} objects layer imports forbidden frontend layer "${match.specifier}"`)
      }

      if (
        filePath.startsWith('frontend/src/objects/shared/') &&
        resolved.startsWith('frontend/src/objects/') &&
        !resolved.startsWith('frontend/src/objects/shared/')
      ) {
        errors.push(`${filePath}:${line} shared objects import domain object "${match.specifier}"`)
      }
    }

    if (filePath.startsWith('frontend/src/system/')) {
      if (hasPrefix(resolved, frontendSystemForbiddenPrefixes)) {
        errors.push(`${filePath}:${line} system layer imports forbidden frontend layer "${match.specifier}"`)
      }

      if (
        resolved.startsWith('frontend/src/objects/') &&
        !resolved.startsWith('frontend/src/objects/shared/')
      ) {
        errors.push(`${filePath}:${line} system layer imports domain object "${match.specifier}"`)
      }
    }

    if (filePath.startsWith('frontend/src/apis/')) {
      if (hasPrefix(resolved, frontendApiForbiddenPrefixes)) {
        errors.push(`${filePath}:${line} api layer imports forbidden frontend layer "${match.specifier}"`)
      }

      if (
        resolved.startsWith('frontend/src/system/') &&
        !resolved.startsWith('frontend/src/system/api/')
      ) {
        errors.push(`${filePath}:${line} api layer imports non-api system module "${match.specifier}"`)
      }
    }

    if (filePath.startsWith('frontend/src/pages/stores/')) {
      if (hasPrefix(resolved, frontendPageStoreForbiddenPrefixes)) {
        errors.push(`${filePath}:${line} page stores import forbidden frontend layer "${match.specifier}"`)
      }

      if (resolved.startsWith('frontend/src/pages/') && !resolved.startsWith('frontend/src/pages/stores/')) {
        errors.push(`${filePath}:${line} page stores import forbidden page layer "${match.specifier}"`)
      }
    }

    if (filePath.startsWith('frontend/src/components/ui/')) {
      if (hasPrefix(resolved, frontendUiForbiddenPrefixes)) {
        errors.push(`${filePath}:${line} ui component imports forbidden frontend layer "${match.specifier}"`)
      }

      if (
        resolved.startsWith('frontend/src/system/') &&
        !resolved.startsWith('frontend/src/system/i18n/')
      ) {
        errors.push(`${filePath}:${line} ui component imports non-i18n system module "${match.specifier}"`)
      }
    }
  }
}

function checkRemovedFrontendResidueImport(filePath, errors) {
  const source = read(filePath)
  for (const match of extractTsSpecifiers(source)) {
    const resolved = resolveFrontendSpecifier(filePath, match.specifier)
    if (resolved.startsWith(removedFrontendFeatureRoot) || resolved.startsWith(removedFrontendSharedRoot)) {
      errors.push(
        `${filePath}:${lineNumber(source, match.index)} imports removed frontend layer "${match.specifier}"`,
      )
    }
  }
}

function addCodecFile(map, domain, basename, filePath) {
  const domainFiles = map.get(domain) ?? new Map()
  domainFiles.set(basename, filePath)
  map.set(domain, domainFiles)
}

function collectFrontendHttpCodecFiles(files, errors) {
  const codecsByDomain = new Map()

  for (const filePath of files) {
    if (frontendApiRootCodecPathPattern.test(filePath)) {
      errors.push(`${filePath} is a misplaced frontend codec file; use apis/<domain>/codecs/*HttpCodecs.ts`)
      continue
    }

    if (frontendApiCodecIndexPathPattern.test(filePath)) {
      errors.push(`${filePath} is a forbidden frontend codec barrel; import codec files directly`)
      continue
    }

    const match = filePath.match(frontendApiCodecFilePattern)
    if (!match) {
      continue
    }

    const [, domain, basename] = match
    if (basename.endsWith('.test')) {
      continue
    }

    if (!frontendApiCodecBasenamePattern.test(basename)) {
      errors.push(`${filePath} must be named *HttpCodecs.ts or *ModelHttpCodecs.ts`)
      continue
    }

    addCodecFile(codecsByDomain, domain, basename, filePath)
  }

  return codecsByDomain
}

function collectBackendHttpCodecFiles(files) {
  const codecsByDomain = new Map()

  for (const filePath of files) {
    const match = filePath.match(backendDomainHttpCodecFilePattern)
    if (!match) {
      continue
    }

    const [, domain, basename] = match
    addCodecFile(codecsByDomain, domain, basename, filePath)
  }

  return codecsByDomain
}

function checkFrontendHttpCodecLayout(frontendFiles, backendFiles, errors) {
  const frontendCodecs = collectFrontendHttpCodecFiles(frontendFiles, errors)
  const backendCodecs = collectBackendHttpCodecFiles(backendFiles)
  const domains = [...new Set([...frontendCodecs.keys(), ...backendCodecs.keys()])].sort()

  for (const domain of domains) {
    const frontendDomainCodecs = frontendCodecs.get(domain) ?? new Map()
    const backendDomainCodecs = backendCodecs.get(domain) ?? new Map()
    const basenames = [...new Set([...frontendDomainCodecs.keys(), ...backendDomainCodecs.keys()])].sort()

    for (const basename of basenames) {
      if (!frontendDomainCodecs.has(basename)) {
        const backendPath = backendDomainCodecs.get(basename)
        errors.push(
          `frontend/src/apis/${domain}/codecs/${basename}.ts is missing matching frontend codec for ${backendPath}`,
        )
      }

      if (!backendDomainCodecs.has(basename)) {
        const frontendPath = frontendDomainCodecs.get(basename)
        errors.push(`${frontendPath} has no matching backend codec basename ${basename}.scala`)
      }
    }
  }
}

function extractScalaImports(source) {
  return source
    .split('\n')
    .map((line, index) => ({ line: line.trim(), lineNumber: index + 1 }))
    .filter((entry) => entry.line.startsWith('import '))
}

function checkBackendObjectFile(filePath, errors) {
  const source = read(filePath)
  const isBareDomainObject = /^backend\/src\/main\/scala\/domains\/[^/]+\/objects\/(?!request\/|response\/)/.test(filePath)
  const isBareSharedObject = /^backend\/src\/main\/scala\/shared\/objects\/(?!request\/|response\/)/.test(filePath)
  for (const entry of extractScalaImports(source)) {
    const importedPath = entry.line.replace(/^import\s+/, '')
    if (hasBlockedSegment(importedPath.replace(/[{}]/g, '.').replace(/,/g, '.'), backendBlockedObjectSegments)) {
      errors.push(`${filePath}:${entry.lineNumber} imports forbidden backend layer "${importedPath}"`)
    }
    if (
      (isBareDomainObject || isBareSharedObject) &&
      /\.objects\.(?:request|response)(?:\.|$|\{)/.test(importedPath)
    ) {
      errors.push(`${filePath}:${entry.lineNumber} imports forbidden backend boundary object "${importedPath}"`)
    }
    if (backendWireCodecImportPattern.test(importedPath)) {
      errors.push(`${filePath}:${entry.lineNumber} imports HTTP wire codec package "${importedPath}"`)
    }
  }

  for (const match of source.matchAll(backendObjectPersistenceHelperPattern)) {
    errors.push(`${filePath}:${lineNumber(source, match.index ?? 0)} defines persistence helper "${match[0].trim()}"`)
  }
}

function checkBackendHttpMapperFile(filePath, errors) {
  const match = filePath.match(backendDomainHttpMapperFilePattern)
  if (!match) {
    return
  }

  const [, , basename] = match
  if (!backendDomainHttpMapperBasenamePattern.test(basename)) {
    errors.push(`${filePath} must be named *HttpRequestMappers.scala or *HttpResponseMappers.scala`)
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

function checkBackendHttpPlansFile(filePath, errors) {
  const source = read(filePath)
  for (const entry of extractScalaImports(source)) {
    const importedPath = entry.line.replace(/^import\s+/, '')
    if (backendDomainTableImportPattern.test(importedPath)) {
      errors.push(`${filePath}:${entry.lineNumber} imports forbidden backend table layer "${importedPath}"`)
    }
  }

  const sourceWithoutImports = source
    .split('\n')
    .map((line) => (line.trim().startsWith('import ') ? '' : line))
    .join('\n')
  for (const match of sourceWithoutImports.matchAll(backendDomainTableReferencePattern)) {
    errors.push(`${filePath}:${lineNumber(sourceWithoutImports, match.index ?? 0)} references forbidden backend table "${match[0]}"`)
  }
}

function extractScalaMethods(source) {
  const methodStartPattern = /^[ \t]*(?:(?:override|private|protected|final)\s+)*def\s+[A-Za-z_$][\w$]*\b/gm
  const starts = [...source.matchAll(methodStartPattern)].map((match) => match.index ?? 0)

  return starts.flatMap((start, index) => {
    const nextStart = starts[index + 1] ?? source.length
    const equalsIndex = source.indexOf('=', start)
    if (equalsIndex === -1 || equalsIndex > nextStart) {
      return []
    }

    return [{
      start,
      signature: source.slice(start, equalsIndex),
      body: source.slice(equalsIndex + 1, nextStart),
    }]
  })
}

function checkBackendEffectfulMethodSignatures(filePath, errors) {
  const source = read(filePath)
  for (const method of extractScalaMethods(source)) {
    if (!backendEffectPattern.test(method.body)) {
      continue
    }

    if (backendEffectfulReturnPattern.test(method.signature)) {
      continue
    }

    const methodName = method.signature.match(/\bdef\s+([A-Za-z_$][\w$]*)\b/)?.[1] ?? '<unknown>'
    errors.push(`${filePath}:${lineNumber(source, method.start)} method "${methodName}" contains side effects but does not return IO[...] or Resource[IO, ...]`)
  }
}

function extractFrontendTopLevelObjectTypes(source) {
  const cleanSource = source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/(^|\s)\/\/.*$/gm, '')

  return [...cleanSource.matchAll(/^export\s+(?:type|interface)\s+([A-Za-z_$][\w$]*)/gm)].map(
    (match) => match[1],
  )
}

function extractBackendTopLevelObjectTypes(source) {
  const cleanSource = source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/(^|\s)\/\/.*$/gm, '')

  return [
    ...cleanSource.matchAll(/^(?:final\s+case\s+class|enum|sealed\s+trait|type)\s+([A-Za-z_$][\w$]*)/gm),
  ].map((match) => match[1])
}

function checkOneTopLevelObjectType(filePath, types, errors) {
  if (types.length === 0) {
    return
  }

  if (types.length > 1) {
    errors.push(`${filePath} defines multiple top-level object types: ${types.join(', ')}`)
    return
  }

  const expectedName = basenameWithoutExtension(filePath)
  if (types[0] !== expectedName) {
    errors.push(`${filePath} defines ${types[0]}, but object file basename must match the type name`)
  }
}

function checkFrontendObjectFileShape(filePath, errors) {
  checkOneTopLevelObjectType(filePath, extractFrontendTopLevelObjectTypes(read(filePath)), errors)
}

function checkBackendObjectFileShape(filePath, errors) {
  checkOneTopLevelObjectType(filePath, extractBackendTopLevelObjectTypes(read(filePath)), errors)
}

function isFrontendObjectContractFile(filePath) {
  const basename = basenameWithoutExtension(filePath)
  return /^[A-Z]/.test(basename) && !basename.endsWith('.test')
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

    if (filePath.startsWith(removedFrontendStoreRoot)) {
      errors.push(`${filePath} is in removed frontend stores layer; use frontend/src/pages/stores`)
    }

    if (filePath.startsWith(removedFrontendFeatureRoot) && /\/domain\//.test(filePath)) {
      errors.push(`${filePath} is in removed frontend feature domain layer`)
    }

    if (filePath.startsWith(removedFrontendFeatureRoot) && /\/http\/(?:request|response)\//.test(filePath)) {
      errors.push(`${filePath} is in removed frontend HTTP contract directory; use objects/<domain>/request or objects/<domain>/response`)
    }

    if (filePath.startsWith(removedFrontendSharedRoot) && /\/http\/response\//.test(filePath)) {
      errors.push(`${filePath} is in removed shared frontend HTTP response directory; use objects/shared/response`)
    }

    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/application\/(?:input|output)\//.test(filePath)) {
      errors.push(`${filePath} is in removed backend application contract directory; use objects/request or objects/response`)
    }

    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/http\/response\//.test(filePath)) {
      errors.push(`${filePath} is in removed backend HTTP response mapper directory; use http/mapper`)
    }

    if (/^backend\/src\/main\/scala\/shared\/http\/response\//.test(filePath)) {
      errors.push(`${filePath} is in removed shared backend HTTP response directory; use shared/objects/response`)
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
  const frontendFiles = [
    ...walk('frontend/src/apis', new Set(['.ts', '.tsx'])),
    ...walk('frontend/src/components/ui', new Set(['.ts', '.tsx'])),
    ...walk('frontend/src/objects', new Set(['.ts', '.tsx'])),
    ...walk('frontend/src/pages', new Set(['.ts', '.tsx'])),
    ...walk('frontend/src/system', new Set(['.ts', '.tsx'])),
    ...walk('frontend/src/test', new Set(['.ts', '.tsx'])),
    ...walk('frontend/src', new Set(['.ts', '.tsx'])).filter((filePath) =>
      /^frontend\/src\/[^/]+\.(?:ts|tsx)$/.test(filePath),
    ),
  ].sort()
  const backendDomainFiles = walk('backend/src/main/scala/domains', new Set(['.scala']))

  for (const filePath of frontendFiles) {
    checkRemovedFrontendResidueImport(filePath, errors)
    checkFrontendLayerFile(filePath, errors)
  }

  checkFrontendHttpCodecLayout(frontendFiles, backendDomainFiles, errors)

  for (const filePath of walk('frontend/src/objects', new Set(['.ts', '.tsx']))) {
    if (
      isFrontendObjectContractFile(filePath) &&
      !/^frontend\/src\/objects\/shared\/access\//.test(filePath)
    ) {
      checkFrontendObjectFileShape(filePath, errors)
    }
  }

  for (const filePath of backendDomainFiles) {
    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/objects\//.test(filePath)) {
      checkBackendObjectFile(filePath, errors)
      checkBackendObjectFileShape(filePath, errors)
    }

    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/application\/(?:input|output)\//.test(filePath)) {
      checkBackendApplicationBoundaryFile(filePath, errors)
    }

    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/http\/.*HttpPlans\.scala$/.test(filePath)) {
      checkBackendHttpPlansFile(filePath, errors)
    }

    if (/^backend\/src\/main\/scala\/domains\/[^/]+\/http\/mapper\//.test(filePath)) {
      checkBackendHttpMapperFile(filePath, errors)
    }

    if (!/^backend\/src\/main\/scala\/domains\/[^/]+\/(?:table|http)\//.test(filePath)) {
      checkBackendEffectfulMethodSignatures(filePath, errors)
    }
  }

  for (const filePath of walk('backend/src/main/scala/shared/objects', new Set(['.scala']))) {
    checkBackendObjectFile(filePath, errors)
    if (!/^backend\/src\/main\/scala\/shared\/objects\/access\//.test(filePath)) {
      checkBackendObjectFileShape(filePath, errors)
    }
  }

  for (const filePath of walk('backend/src/main/scala/shared', new Set(['.scala']))) {
    if (!/^backend\/src\/main\/scala\/shared\/(?:objects|http)\//.test(filePath)) {
      checkBackendEffectfulMethodSignatures(filePath, errors)
    }
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
