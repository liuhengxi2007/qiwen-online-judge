import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { dirname, extname, join, relative, resolve, sep } from 'node:path'

const root = process.cwd()

const allowedMissing = new Set([
  'backend-only:blog/BlogCommentNotificationAncestor',
  'backend-only:blog/BlogCommentNotificationContext',
  'backend-only:message/MarkConversationReadMode',
  'backend-only:problem/ProblemAccessEvaluation',
  'backend-only:problem/ProblemSetMemberTarget',
  'backend-only:shared/ResourceAccessDecision',
  'backend-only:shared/ResourceAccessFacts',
  'backend-only:submission/ClaimedSubmission',
])

const allowedFieldMismatch = new Set([
  // Example: 'field-mismatch:problem/SomeBoundaryType'
])

const allowedUnionMismatch = new Set([
  // Example: 'union-mismatch:submission/SubmissionStatus'
])

const ignoredKeys = new Set([
  'auth/AuthUser',
  'submission/SubmissionJudgeCompletion',
  'submission/SubmissionJudgeState',
])

const frontendRoots = [
  'frontend/src/features',
  'frontend/src/shared/model',
  'frontend/src/shared/access',
]

const backendRoots = [
  'backend/src/main/scala/domains',
  'backend/src/main/scala/shared/model',
  'backend/src/main/scala/shared/access',
  'backend/src/main/scala/shared/http/response',
]

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
}

function walk(relativePath, extension) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    return []
  }

  const entries = readdirSync(absolutePath).flatMap((entry) => {
    const child = join(relativePath, entry)
    const childAbsolute = resolve(root, child)
    if (statSync(childAbsolute).isDirectory()) {
      return walk(child, extension)
    }
    return extname(child) === extension ? [child] : []
  })

  return entries.sort()
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

function frontendMetadata(path) {
  const normalized = path.split(sep).join('/')
  const featureMatch = normalized.match(/^frontend\/src\/features\/([^/]+)\/(http\/request|http\/response|model)\//)
  if (featureMatch) {
    return {
      scope: featureMatch[1],
      required: featureMatch[2] !== 'model',
    }
  }

  if (normalized.startsWith('frontend/src/shared/model/') || normalized.startsWith('frontend/src/shared/access/')) {
    return { scope: 'shared', required: true }
  }

  return null
}

function backendMetadata(path) {
  const normalized = path.split(sep).join('/')
  const domainMatch = normalized.match(/^backend\/src\/main\/scala\/domains\/([^/]+)\/(application\/input|application\/output|model)\//)
  if (domainMatch) {
    return {
      scope: domainMatch[1],
      required: domainMatch[2] !== 'model',
    }
  }

  if (
    normalized.startsWith('backend/src/main/scala/shared/model/') ||
    normalized.startsWith('backend/src/main/scala/shared/access/') ||
    normalized.startsWith('backend/src/main/scala/shared/http/response/')
  ) {
    return { scope: 'shared', required: true }
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

function collectFrontend() {
  const scopedTypes = new Map()
  const globalTypes = new Map()
  const files = frontendRoots
    .flatMap((path) => walk(path, '.ts'))
    .filter((path) => frontendMetadata(path))

  for (const path of files) {
    const metadata = frontendMetadata(path)
    const exportedTypes = extractExportedTsTypes(read(path))
    for (const [name, body] of exportedTypes) {
      const entry = {
        side: 'frontend',
        scope: metadata.scope,
        name,
        key: `${metadata.scope}/${name}`,
        path,
        body,
        required: metadata.required,
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

  return scopedTypes
}

function collectBackend() {
  const scopedTypes = new Map()
  const files = backendRoots
    .flatMap((path) => walk(path, '.scala'))
    .filter((path) => backendMetadata(path))

  for (const path of files) {
    const metadata = backendMetadata(path)
    const exportedTypes = extractScalaTypes(read(path))
    for (const [name, entry] of exportedTypes) {
      scopedTypes.set(`${metadata.scope}/${name}`, {
        side: 'backend',
        scope: metadata.scope,
        name,
        key: `${metadata.scope}/${name}`,
        path,
        required: metadata.required,
        ...entry,
      })
    }
  }

  return scopedTypes
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

function isFrontendDiscriminatedUnionVariant(entry) {
  return entry?.side === 'frontend' && entry.kind === 'object' && entry.fields.includes('kind')
}

function comparePair(key, frontendEntry, backendEntry, errors) {
  if (!frontendEntry || !backendEntry) {
    return
  }

  if (frontendEntry.kind === 'object' && backendEntry.kind === 'object') {
    const exceptionKey = `field-mismatch:${key}`
    if (!sameList(frontendEntry.fields, backendEntry.fields) && !allowedFieldMismatch.has(exceptionKey)) {
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
    if (!sameList(frontendEntry.union, backendEntry.union) && !allowedUnionMismatch.has(exceptionKey)) {
      errors.push(
        `${key}: frontend union [${joined(frontendEntry.union)}] does not match backend union [${joined(
          backendEntry.union,
        )}]`,
      )
    }
  }
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
  if (exceptionKeys.some((exceptionKey) => allowedMissing.has(exceptionKey))) {
    return
  }

  errors.push(`${key}: missing ${missingSide}; found ${sideLabel(presentSide, frontendEntry ?? backendEntry)}`)
}

function run() {
  const errors = []
  const frontend = collectFrontend()
  const backend = collectBackend()
  const allKeys = [...new Set([...frontend.keys(), ...backend.keys()])].sort()

  for (const key of allKeys) {
    if (ignoredKeys.has(key)) {
      continue
    }

    const frontendEntry = frontend.get(key)
    const backendEntry = backend.get(key)
    checkMissing(key, frontendEntry, backendEntry, errors)
    comparePair(key, frontendEntry, backendEntry, errors)
  }

  if (errors.length > 0) {
    console.error(`Frontend/backend alignment check failed with ${errors.length} issue(s):`)
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exit(1)
  }

  console.log(`Frontend/backend alignment check passed for ${allKeys.length} discovered type key(s).`)
}

run()
