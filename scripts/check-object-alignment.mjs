import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { basename, extname, join, relative, resolve } from 'node:path'

const root = process.cwd()

const allowedExceptions = new Map([
  [
    'backend-only:auth/ProblemManagerUser',
    'Backend-only problem-manager permission proof wraps the authenticated actor for server authorization and is not serialized as a frontend JSON payload.',
  ],
  [
    'backend-only:auth/SessionToken',
    'Backend-only session token value is handled through cookie/cache plumbing and should not be mirrored as a frontend object payload.',
  ],
  [
    'backend-only:auth/SiteManagerUser',
    'Backend-only site-manager permission proof wraps the authenticated actor for server authorization and is not serialized as a frontend JSON payload.',
  ],
  [
    'frontend-only:submission/JudgeFailureReason',
    'Frontend submission detail mirrors judge-protocol JudgeFailureReason, which is shared through the judge protocol module rather than backend domain objects.',
  ],
  [
    'frontend-only:submission/JudgeResult',
    'Frontend submission detail mirrors judge-protocol JudgeResult, which is shared through the judge protocol module rather than backend domain objects.',
  ],
  [
    'frontend-only:submission/JudgeResultMetrics',
    'Frontend submission detail mirrors judge-protocol JudgeResultMetrics, which is shared through the judge protocol module rather than backend domain objects.',
  ],
  [
    'frontend-only:submission/JudgeSubtaskResult',
    'Frontend submission detail mirrors judge-protocol JudgeSubtaskResult, which is shared through the judge protocol module rather than backend domain objects.',
  ],
  [
    'frontend-only:submission/JudgeTestcaseResult',
    'Frontend submission detail mirrors judge-protocol JudgeTestcaseResult, which is shared through the judge protocol module rather than backend domain objects.',
  ],
  // Example: ['field-mismatch:problem/SomeBoundaryType', 'Reason this shape intentionally differs.'],
])
const usedExceptions = new Set()

const scopedObjectSubdirectories = ['request', 'response']
const backendInternalFrontendPayloadMirrors = new Map([
  ['problem', new Set(['ProblemDataManifest', 'ProblemDataManifestEntry'])],
  ['submission', new Set(['ClaimedSubmission', 'SubmissionJudgeState'])],
])

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
}

function sortedDirectoryEntries(relativePath) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    return []
  }

  return readdirSync(absolutePath, { withFileTypes: true }).sort((left, right) => left.name.localeCompare(right.name))
}

function directDirectories(relativePath) {
  return sortedDirectoryEntries(relativePath)
    .filter((entry) => entry.isDirectory())
    .map((entry) => ({
      name: entry.name,
      path: join(relativePath, entry.name),
    }))
}

function directFiles(relativePath, extension) {
  return sortedDirectoryEntries(relativePath)
    .filter((entry) => entry.isFile() && extname(entry.name) === extension)
    .map((entry) => join(relativePath, entry.name))
}

function objectFileName(path) {
  return basename(path, extname(path))
}

function objectFileKey(scope, subdirectory, path) {
  return [scope, subdirectory, objectFileName(path)].filter(Boolean).join('/')
}

function indexObjectFiles(files) {
  return new Map(files.map((file) => [file.key, file]))
}

function collectScopedObjectFiles(side, scope, objectRoot, extension) {
  const files = directFiles(objectRoot, extension).map((path) => ({
    side,
    scope,
    subdirectory: null,
    name: objectFileName(path),
    key: objectFileKey(scope, null, path),
    path,
    required: false,
  }))

  for (const subdirectory of scopedObjectSubdirectories) {
    files.push(
      ...directFiles(join(objectRoot, subdirectory), extension).map((path) => ({
        side,
        scope,
        subdirectory,
        name: objectFileName(path),
        key: objectFileKey(scope, subdirectory, path),
        path,
        required: true,
      })),
    )
  }

  return files
}

function collectFrontendObjectFiles() {
  const frontendObjectsRoot = 'frontend/src/objects'
  return indexObjectFiles(
    directDirectories(frontendObjectsRoot).flatMap((directory) =>
      collectScopedObjectFiles('frontend', directory.name, directory.path, '.ts'),
    ),
  )
}

function collectBackendObjectFiles() {
  const backendDomainsRoot = 'backend/src/main/scala/domains'
  const backendSharedObjectsRoot = 'backend/src/main/scala/shared/objects'
  const domainDirectories = directDirectories(backendDomainsRoot)
  const domainFiles = domainDirectories.flatMap((directory) =>
    collectScopedObjectFiles('backend', directory.name, join(directory.path, 'objects'), '.scala'),
  )
  const internalPayloadFiles = domainDirectories.flatMap((directory) =>
    collectBackendInternalFrontendPayloadFiles(directory),
  )

  return indexObjectFiles([
    ...domainFiles,
    ...internalPayloadFiles,
    ...collectScopedObjectFiles('backend', 'shared', backendSharedObjectsRoot, '.scala'),
  ])
}

function collectBackendInternalFrontendPayloadFiles(directory) {
  const mirroredNames = backendInternalFrontendPayloadMirrors.get(directory.name)
  if (!mirroredNames) {
    return []
  }

  return directFiles(join(directory.path, 'objects', 'internal'), '.scala')
    .filter((path) => mirroredNames.has(objectFileName(path)))
    .map((path) => ({
      side: 'backend',
      scope: directory.name,
      subdirectory: null,
      name: objectFileName(path),
      key: objectFileKey(directory.name, null, path),
      path,
      required: false,
    }))
}

function stripComments(source) {
  return source
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/(^|\s)\/\/.*$/gm, '')
}

function splitTopLevel(value, delimiter) {
  const parts = []
  let current = ''
  let angleDepth = 0
  let braceDepth = 0
  let bracketDepth = 0
  let parenDepth = 0
  let quote = null

  for (let index = 0; index < value.length; index += 1) {
    const char = value[index]
    const previous = value[index - 1]

    if (quote) {
      current += char
      if (char === quote && previous !== '\\') {
        quote = null
      }
      continue
    }

    if (char === "'" || char === '"' || char === '`') {
      quote = char
      current += char
      continue
    }

    if (char === '<') angleDepth += 1
    if (char === '>') angleDepth = Math.max(0, angleDepth - 1)
    if (char === '{') braceDepth += 1
    if (char === '}') braceDepth = Math.max(0, braceDepth - 1)
    if (char === '[') bracketDepth += 1
    if (char === ']') bracketDepth = Math.max(0, bracketDepth - 1)
    if (char === '(') parenDepth += 1
    if (char === ')') parenDepth = Math.max(0, parenDepth - 1)

    if (
      char === delimiter &&
      angleDepth === 0 &&
      braceDepth === 0 &&
      bracketDepth === 0 &&
      parenDepth === 0
    ) {
      parts.push(current.trim())
      current = ''
      continue
    }

    current += char
  }

  if (current.trim()) {
    parts.push(current.trim())
  }

  return parts
}

function extractObjectBody(value) {
  const start = value.indexOf('{')
  if (start < 0) {
    return null
  }

  let depth = 0
  let quote = null
  for (let index = start; index < value.length; index += 1) {
    const char = value[index]
    const previous = value[index - 1]

    if (quote) {
      if (char === quote && previous !== '\\') {
        quote = null
      }
      continue
    }

    if (char === "'" || char === '"' || char === '`') {
      quote = char
      continue
    }

    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return value.slice(start + 1, index)
      }
    }
  }

  return null
}

function normalizeFieldName(field) {
  return field.trim().replace(/^readonly\s+/, '').replace(/[?'"]+$/g, '').replace(/^['"]/, '')
}

function extractTsFieldsFromObjectBody(body) {
  return splitTopLevel(body, '\n')
    .flatMap((line) => splitTopLevel(line, ';'))
    .flatMap((line) => splitTopLevel(line, ','))
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !line.startsWith('|'))
    .filter((line) => !line.startsWith('readonly __brand'))
    .map((line) => line.match(/^([A-Za-z_$][\w$]*|['"][^'"]+['"])\??\s*:/)?.[1])
    .filter(Boolean)
    .map(normalizeFieldName)
}

function extractExportedTsTypes(source) {
  const cleanSource = stripComments(source)
  const types = new Map()
  const exportPattern = /export\s+(?:type|interface)\s+([A-Za-z_$][\w$]*)(?:<[^=/{]+>)?\s*(?:=)?\s*/g
  let match

  while ((match = exportPattern.exec(cleanSource))) {
    const name = match[1]
    const bodyStart = exportPattern.lastIndex
    const next = cleanSource.slice(bodyStart).search(/\nexport\s+(?:type|interface)\s+/)
    const rawBody = (next < 0 ? cleanSource.slice(bodyStart) : cleanSource.slice(bodyStart, bodyStart + next)).trim()
    types.set(name, rawBody)
  }

  return types
}

function extractTsLiteralUnion(body) {
  const withoutObjects = body.replace(/\{[\s\S]*?\}/g, '')
  const literals = [...withoutObjects.matchAll(/'([^']+)'|"([^"]+)"/g)].map((entry) => entry[1] ?? entry[2])
  if (literals.length === 0) {
    return null
  }

  const nonLiteralParts = splitTopLevel(withoutObjects, '|')
    .map((part) => part.trim())
    .filter(Boolean)
    .filter((part) => !/^['"][^'"]+['"]$/.test(part))

  return nonLiteralParts.length === 0 ? literals : null
}

function aliasTarget(body) {
  const match = body.match(/^([A-Za-z_$][\w$]*)(?:<[^>]+>)?$/)
  return match?.[1] ?? null
}

function resolveTsFields(typeName, allTypes, seen = new Set()) {
  if (seen.has(typeName)) {
    return null
  }
  seen.add(typeName)

  const entry = allTypes.get(typeName)
  if (!entry) {
    return null
  }

  if (entry.fields) {
    return entry.fields
  }

  const body = entry.body
  if (/^(string|number|boolean|unknown|never|null|undefined)\b/.test(body)) {
    return null
  }

  if (/^PageResponse</.test(body)) {
    entry.fields = ['items', 'page', 'pageSize', 'totalItems']
    return entry.fields
  }

  const unionParts = splitTopLevel(body, '|').filter(Boolean)
  if (unionParts.length > 1) {
    const unionFields = []
    let allObjectParts = true
    for (const part of unionParts) {
      const objectBody = extractObjectBody(part)
      if (!objectBody) {
        allObjectParts = false
        break
      }

      for (const field of extractTsFieldsFromObjectBody(objectBody)) {
        if (!unionFields.includes(field)) {
          unionFields.push(field)
        }
      }
    }

    if (allObjectParts && unionFields.length > 0) {
      entry.fields = unionFields
      return entry.fields
    }
  }

  const ownFields = []
  const inheritedFields = []
  for (const part of splitTopLevel(body, '&')) {
    const objectBody = extractObjectBody(part)
    if (objectBody) {
      ownFields.push(...extractTsFieldsFromObjectBody(objectBody))
      continue
    }

    const target = aliasTarget(part)
    if (target) {
      const targetFields = resolveTsFields(target, allTypes, seen)
      if (targetFields) {
        inheritedFields.push(...targetFields)
      }
    }
  }

  const fields = [...ownFields, ...inheritedFields]
  if (fields.length > 0) {
    entry.fields = fields
    return fields
  }

  const target = aliasTarget(body)
  if (target) {
    return resolveTsFields(target, allTypes, seen)
  }

  return null
}

function extractScalaFields(params) {
  return splitTopLevel(params, ',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.replace(/^(?:private\s+)?(?:val\s+|var\s+)?/, ''))
    .map((part) => normalizeFieldName(part.split(':')[0].split('=')[0]))
    .filter(Boolean)
}

function extractScalaStringCases(source, typeName) {
  const objectPattern = new RegExp(`object\\s+${typeName}\\s*:[\\s\\S]*?(?=\\n(?:final\\s+case\\s+class|enum|sealed\\s+trait|object)\\s+|$)`)
  const objectSource = source.match(objectPattern)?.[0] ?? source
  const databaseCases = [...objectSource.matchAll(/case\s+"([^"]+)"\s*=>\s*(?:Right|Some)\(/g)].map((entry) => entry[1])
  if (databaseCases.length > 0) {
    return [...new Set(databaseCases)]
  }

  const encodedCases = [...objectSource.matchAll(/=>\s*"([^"]+)"/g)].map((entry) => entry[1])
  if (encodedCases.length > 0) {
    return [...new Set(encodedCases)]
  }

  const jsonKindCases = [...objectSource.matchAll(/"kind"\s*->\s*Json\.fromString\("([^"]+)"\)/g)].map((entry) => entry[1])
  if (jsonKindCases.length > 0) {
    return [...new Set(jsonKindCases)]
  }

  return null
}

function extractScalaTypes(source) {
  const cleanSource = stripComments(source)
  const types = new Map()

  for (const match of cleanSource.matchAll(/^final\s+case\s+class\s+([A-Za-z_$][\w$]*)(?:\[[^\]]+\])?(?:\s+private)?\s*\(([\s\S]*?)\)(?=\s*(?:extends|:|=|\{|$|\nobject|\nfinal|\nenum|\nsealed))/gm)) {
    types.set(match[1], {
      kind: 'object',
      fields: extractScalaFields(match[2]),
    })
  }

  for (const match of cleanSource.matchAll(/^enum\s+([A-Za-z_$][\w$]*)\s*:/gm)) {
    const union = extractScalaStringCases(cleanSource, match[1])
    if (union) {
      types.set(match[1], { kind: 'union', union })
    }
  }

  for (const match of cleanSource.matchAll(/^sealed\s+trait\s+([A-Za-z_$][\w$]*)/gm)) {
    const union = extractScalaStringCases(cleanSource, match[1])
    if (union) {
      types.set(match[1], { kind: 'union', union })
    }
  }

  for (const match of cleanSource.matchAll(/^type\s+([A-Za-z_$][\w$]*)\s*=\s*([^\n]+)/gm)) {
    const name = match[1]
    const target = match[2].trim()
    if (/^PageResponse\[/.test(target)) {
      types.set(name, {
        kind: 'object',
        fields: ['items', 'page', 'pageSize', 'totalItems'],
      })
    }
  }

  return types
}

function typeKey(file, typeName) {
  return typeName === file.name ? file.key : `${file.key}/${typeName}`
}

function collectFrontend() {
  const files = collectFrontendObjectFiles()
  const scopedTypes = new Map()
  const globalTypes = new Map()

  for (const file of files.values()) {
    const exportedTypes = extractExportedTsTypes(read(file.path))
    for (const [name, body] of exportedTypes) {
      const entry = {
        side: 'frontend',
        scope: file.scope,
        name,
        key: typeKey(file, name),
        fileKey: file.key,
        path: file.path,
        body,
        required: file.required,
      }
      globalTypes.set(name, entry)
      scopedTypes.set(entry.key, entry)
    }
  }

  for (const entry of scopedTypes.values()) {
    const literalUnion = extractTsLiteralUnion(entry.body)
    const fields = resolveTsFields(entry.name, globalTypes)
    if (literalUnion) {
      entry.kind = 'union'
      entry.union = literalUnion
    } else if (fields) {
      entry.kind = 'object'
      entry.fields = fields
    } else {
      entry.kind = 'alias'
    }
  }

  return { files, types: scopedTypes }
}

function collectBackend() {
  const files = collectBackendObjectFiles()
  const scopedTypes = new Map()

  for (const file of files.values()) {
    const exportedTypes = extractScalaTypes(read(file.path))
    for (const [name, entry] of exportedTypes) {
      const key = typeKey(file, name)
      scopedTypes.set(key, {
        side: 'backend',
        scope: file.scope,
        name,
        key,
        fileKey: file.key,
        path: file.path,
        required: file.required,
        ...entry,
      })
    }
  }

  return { files, types: scopedTypes }
}

function sideLabel(side, entry) {
  return entry ? `${side}:${relative(root, resolve(root, entry.path))}` : side
}

function joined(value) {
  return value.join(', ')
}

function sameList(left, right) {
  return joined(left) === joined(right)
}

function validateAllowedExceptions(errors) {
  for (const [exceptionKey, reason] of allowedExceptions) {
    if (typeof reason !== 'string' || reason.trim().length === 0) {
      errors.push(`${exceptionKey}: object alignment exception must include a non-empty reason`)
    }
  }
}

function useAllowedException(exceptionKey) {
  if (!allowedExceptions.has(exceptionKey)) {
    return false
  }

  usedExceptions.add(exceptionKey)
  return true
}

function checkUnusedAllowedExceptions(errors) {
  for (const exceptionKey of allowedExceptions.keys()) {
    if (!usedExceptions.has(exceptionKey)) {
      errors.push(`${exceptionKey}: unused object alignment exception`)
    }
  }
}

function isFrontendDiscriminatedUnionVariant(entry) {
  return entry?.side === 'frontend' && entry.kind === 'object' && entry.fields.includes('kind')
}

function comparePair(key, frontendEntry, backendEntry, errors) {
  if (!frontendEntry || !backendEntry) {
    return
  }

  if (frontendEntry.kind === 'object' && backendEntry.kind === 'object') {
    const exceptionKey = `field-mismatch:${key}`
    if (!sameList(frontendEntry.fields, backendEntry.fields)) {
      if (useAllowedException(exceptionKey)) {
        return
      }

      errors.push(
        `${key}: frontend fields [${joined(frontendEntry.fields)}] do not match backend fields [${joined(
          backendEntry.fields,
        )}]`,
      )
    }
    return
  }

  if (frontendEntry.kind === 'union' && backendEntry.kind === 'union') {
    const exceptionKey = `union-mismatch:${key}`
    if (!sameList(frontendEntry.union, backendEntry.union)) {
      if (useAllowedException(exceptionKey)) {
        return
      }

      errors.push(
        `${key}: frontend union [${joined(frontendEntry.union)}] does not match backend union [${joined(
          backendEntry.union,
        )}]`,
      )
    }
  }
}

function checkFileAlignment(frontendFiles, backendFiles, errors) {
  const allFileKeys = [...new Set([...frontendFiles.keys(), ...backendFiles.keys()])].sort()

  for (const key of allFileKeys) {
    const frontendFile = frontendFiles.get(key)
    const backendFile = backendFiles.get(key)
    if (frontendFile && !backendFile) {
      if (useAllowedException(`frontend-only:${key}`)) {
        continue
      }

      errors.push(`${key}: frontend-only object file ${sideLabel('frontend', frontendFile)}`)
      continue
    }

    if (!frontendFile && backendFile) {
      if (useAllowedException(`backend-only:${key}`)) {
        continue
      }

      errors.push(`${key}: backend-only object file ${sideLabel('backend', backendFile)}`)
    }
  }
}

function intersectionSet(left, right) {
  const rightKeys = new Set(right)
  return new Set([...left].filter((key) => rightKeys.has(key)))
}

function filterTypesByFileKeys(types, fileKeys) {
  const filtered = new Map()
  for (const [key, entry] of types) {
    if (fileKeys.has(entry.fileKey)) {
      filtered.set(key, entry)
    }
  }
  return filtered
}

function checkMissing(key, frontendEntry, backendEntry, errors) {
  if (frontendEntry && !backendEntry && isFrontendDiscriminatedUnionVariant(frontendEntry)) {
    return
  }

  const required = Boolean(frontendEntry?.required || backendEntry?.required)
  if (!required || (frontendEntry && backendEntry)) {
    return
  }

  const presentSide = frontendEntry ? 'frontend' : 'backend'
  const missingSide = frontendEntry ? 'backend' : 'frontend'
  const exceptionKeys = [`missing-${missingSide}:${key}`, `${presentSide}-only:${key}`]
  if (exceptionKeys.some((exceptionKey) => useAllowedException(exceptionKey))) {
    return
  }

  errors.push(`${key}: missing ${missingSide}; found ${sideLabel(presentSide, frontendEntry ?? backendEntry)}`)
}

function run() {
  const errors = []
  validateAllowedExceptions(errors)
  const frontend = collectFrontend()
  const backend = collectBackend()
  checkFileAlignment(frontend.files, backend.files, errors)

  const matchedFileKeys = intersectionSet(frontend.files.keys(), backend.files.keys())
  const frontendTypes = filterTypesByFileKeys(frontend.types, matchedFileKeys)
  const backendTypes = filterTypesByFileKeys(backend.types, matchedFileKeys)
  const allTypeKeys = [...new Set([...frontendTypes.keys(), ...backendTypes.keys()])].sort()

  for (const key of allTypeKeys) {
    const frontendEntry = frontendTypes.get(key)
    const backendEntry = backendTypes.get(key)
    checkMissing(key, frontendEntry, backendEntry, errors)
    comparePair(key, frontendEntry, backendEntry, errors)
  }

  checkUnusedAllowedExceptions(errors)

  if (errors.length > 0) {
    console.error(`Frontend/backend object alignment check failed with ${errors.length} issue(s):`)
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exit(1)
  }

  console.log(
    `Frontend/backend object alignment check passed for ${allTypeKeys.length} discovered type key(s) across ${
      matchedFileKeys.size
    } matched object file key(s).`,
  )
}

run()
