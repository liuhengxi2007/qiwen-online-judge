#!/usr/bin/env node

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { extname, join, resolve, sep } from 'node:path'

const root = process.cwd()
const checkedRoots = [
  'backend/src/main/scala/domains',
  'backend/src/test/scala/domains',
]

const checkedSharedRoots = [
  'backend/src/main/scala/shared',
  'backend/src/test/scala/shared',
]

const publicAuthApiObjectProtocol = new Set([
  'domains.auth.api.ApiObjectContext',
  'domains.auth.api.ApiObjectRouter',
  'domains.auth.api.AuthenticatedApi',
  'domains.auth.api.AuthenticatedResponseApi',
  'domains.auth.api.InternalOnlyApi',
  'domains.auth.api.InternalOnlyAuthenticatedApi',
  'domains.auth.api.PublicApi',
  'domains.auth.api.PublicResponseApi',
  'domains.auth.api.SessionResolver',
  'domains.auth.api.SiteManagerApi',
])

const publicWiringTypes = new Set([
  'domains.auth.utils.SessionStoreContext',
])

const explicitCollaborationBoundaries = new Set([
  'domains.contest.objects.response.EvaluateContestAccessResult',
  'domains.hack.objects.internal.ClaimedHackAttempt',
  'domains.judge.utils.JudgeConfig',
  'domains.judge.utils.JudgeTaskBuilder',
  'domains.judge.utils.JudgeTokenAuth',
  'domains.notification.objects.NotificationKind',
  'domains.notification.objects.NotificationPayload',
  'domains.notification.utils.NotificationEventHub',
  'domains.notification.utils.NotificationEventHubContext',
  'domains.notification.utils.NotificationStreamEvent',
  'domains.problem.utils.ProblemDataStorage',
  'domains.problem.utils.ProblemDataStorageContext',
  'domains.submission.objects.SubmissionStatus',
  'domains.submission.objects.SubmissionVerdict',
  'domains.submission.objects.internal.ClaimedSubmission',
  'domains.submission.objects.internal.SubmissionProgramManifest',
  'domains.submission.objects.internal.SubmissionJudgeCompletion',
  'domains.submission.objects.internal.SubmissionJudgeState',
  'domains.submission.utils.SubmissionProgramCleanup',
  'domains.submission.utils.SubmissionProgramStorage',
  'domains.submission.utils.SubmissionProgramStorageContext',
  'domains.submission.utils.SubmissionJudgeRules',
])

const publicWiringBoundaries = new Set([
  ...publicAuthApiObjectProtocol,
  ...publicWiringTypes,
])

const publicModelTypes = new Map([
  ['auth', new Set([
    'AuthPermissionFlags',
    'AuthenticatedUser',
    'EmailAddress',
    'PasswordHash',
    'PlaintextPassword',
    'SessionToken',
    'SiteManagerUser',
  ])],
  ['blog', new Set([
    // Internal notification context is intentionally consumed by the notification domain.
    'BlogCommentNotificationContext',
    'BlogCommentId',
    'BlogId',
    'BlogTitle',
  ])],
  ['contest', new Set([
    'ContestId',
    'ContestSlug',
    'ContestTitle',
  ])],
  ['hack', new Set([
    'HackId',
  ])],
  ['message', new Set([
    'MessageConversationId',
    'MessageId',
  ])],
  ['notification', new Set([
    'NotificationId',
  ])],
  ['problem', new Set([
    'OtherUserSubmissionAccess',
    'ProblemData',
    'ProblemDataFilename',
    'ProblemDataManifest',
    'ProblemDataManifestEntry',
    'ProblemDataPath',
    'ProblemDetail',
    'ProblemId',
    'ProblemSlug',
    'ProblemStatementText',
    'ProblemTitle',
    'ProblemTitleDisplayMode',
  ])],
  ['problemset', new Set([
    'ProblemSetDescription',
    'ProblemSetId',
    'ProblemSetProblemSummary',
    'ProblemSetSlug',
    'ProblemSetTitle',
  ])],
  ['rating', new Set([
    'RatingValue',
  ])],
  ['submission', new Set([
    // Internal claimed-task shape is intentionally consumed by the judge domain.
    'ClaimedSubmission',
    'SubmissionId',
    'SubmissionLanguage',
    'SubmissionResultDisplayMode',
    'SubmissionSourceCode',
  ])],
  ['user', new Set([
    'DisplayName',
    'UserAvatarUrl',
    'UserDisplayMode',
    'UserIdentity',
    'UserLocale',
    'UserProfileSettings',
    'UserPreferences',
    'Username',
  ])],
  ['usergroup', new Set([
    'NewUserGroupMemberRole',
    'UserGroupDescription',
    'UserGroupName',
    'UserGroupRole',
    'UserGroupSlug',
  ])],
])

function normalizePath(path) {
  return path.split(sep).join('/')
}

function read(relativePath) {
  return readFileSync(resolve(root, relativePath), 'utf8').replace(/\r\n/g, '\n')
}

function walk(relativePath, extension) {
  const absolutePath = resolve(root, relativePath)
  if (!existsSync(absolutePath)) {
    return []
  }

  return readdirSync(absolutePath)
    .flatMap((entry) => {
      const child = join(relativePath, entry)
      const childAbsolute = resolve(root, child)
      if (statSync(childAbsolute).isDirectory()) {
        return walk(child, extension)
      }
      return extname(child) === extension ? [normalizePath(child)] : []
    })
    .sort()
}

function sourceDomain(filePath) {
  return filePath.match(/^backend\/src\/(?:main|test)\/scala\/domains\/([^/]+)\//)?.[1] ?? null
}

function stripComments(source) {
  let clean = ''
  let index = 0
  let inBlockComment = false
  let inLineComment = false
  let inString = false
  let stringDelimiter = ''

  while (index < source.length) {
    const char = source[index]
    const next = source[index + 1]

    if (inLineComment) {
      if (char === '\n') {
        inLineComment = false
        clean += char
      } else {
        clean += ' '
      }
      index += 1
      continue
    }

    if (inBlockComment) {
      if (char === '*' && next === '/') {
        clean += '  '
        index += 2
        inBlockComment = false
      } else {
        clean += char === '\n' ? '\n' : ' '
        index += 1
      }
      continue
    }

    if (inString) {
      clean += char
      if (char === '\\' && stringDelimiter !== '"""') {
        clean += next ?? ''
        index += 2
        continue
      }
      if (stringDelimiter === '"""' && source.slice(index, index + 3) === '"""') {
        clean += source.slice(index + 1, index + 3)
        index += 3
        inString = false
        stringDelimiter = ''
        continue
      }
      if (stringDelimiter !== '"""' && char === stringDelimiter) {
        inString = false
        stringDelimiter = ''
      }
      index += 1
      continue
    }

    if (char === '/' && next === '/') {
      clean += '  '
      index += 2
      inLineComment = true
      continue
    }

    if (char === '/' && next === '*') {
      clean += '  '
      index += 2
      inBlockComment = true
      continue
    }

    if (source.slice(index, index + 3) === '"""') {
      clean += '"""'
      index += 3
      inString = true
      stringDelimiter = '"""'
      continue
    }

    if (char === '"' || char === "'") {
      clean += char
      index += 1
      inString = true
      stringDelimiter = char
      continue
    }

    clean += char
    index += 1
  }

  return clean
}

function braceBalance(text) {
  let balance = 0
  for (const char of text) {
    if (char === '{') {
      balance += 1
    } else if (char === '}') {
      balance -= 1
    }
  }
  return balance
}

function collectScalaImports(source) {
  const lines = stripComments(source).split('\n')
  const imports = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]
    if (!line.trimStart().startsWith('import ')) {
      index += 1
      continue
    }

    const startLine = index + 1
    const collected = [line.trim()]
    let balance = braceBalance(line)
    index += 1

    while (index < lines.length && balance > 0) {
      collected.push(lines[index].trim())
      balance += braceBalance(lines[index])
      index += 1
    }

    imports.push({
      line: startLine,
      statement: collected.join(' '),
    })
  }

  return imports
}

function splitTopLevel(value, separator) {
  const parts = []
  let start = 0
  let balance = 0

  for (let index = 0; index < value.length; index += 1) {
    const char = value[index]
    if (char === '{') {
      balance += 1
    } else if (char === '}') {
      balance -= 1
    } else if (char === separator && balance === 0) {
      parts.push(value.slice(start, index).trim())
      start = index + 1
    }
  }

  parts.push(value.slice(start).trim())
  return parts.filter(Boolean)
}

function matchingBraceIndex(value, openIndex) {
  let balance = 0
  for (let index = openIndex; index < value.length; index += 1) {
    const char = value[index]
    if (char === '{') {
      balance += 1
    } else if (char === '}') {
      balance -= 1
      if (balance === 0) {
        return index
      }
    }
  }
  return -1
}

function stripRename(value) {
  const arrowMatch = value.match(/^(.*?)\s*=>\s*([A-Za-z_$*][\w$]*|_)\s*$/)
  if (arrowMatch) {
    return arrowMatch[2] === '_' ? null : arrowMatch[1].trim()
  }

  const asMatch = value.match(/^(.*?)\s+as\s+([A-Za-z_$*][\w$]*|_)\s*$/)
  if (asMatch) {
    return asMatch[2] === '_' ? null : asMatch[1].trim()
  }

  return value.trim()
}

function normalizeImportPath(value) {
  return value
    .replace(/\s+/g, '')
    .replace(/\._$/, '.*')
    .replace(/\.{2,}/g, '.')
    .replace(/^\./, '')
    .replace(/\.$/, '')
}

function expandImportExpression(expression) {
  const trimmed = expression.trim()
  const openIndex = trimmed.indexOf('{')
  if (openIndex === -1) {
    const original = stripRename(trimmed)
    return original === null ? [] : [normalizeImportPath(original)]
  }

  const closeIndex = matchingBraceIndex(trimmed, openIndex)
  if (closeIndex === -1) {
    return [normalizeImportPath(trimmed)]
  }

  const prefix = trimmed.slice(0, openIndex).trim().replace(/\.$/, '')
  const content = trimmed.slice(openIndex + 1, closeIndex)
  const suffix = trimmed.slice(closeIndex + 1).trim().replace(/^\./, '')

  return splitTopLevel(content, ',').flatMap((selector) => {
    const expandedPrefix = `${prefix}.${selector}`.replace(/^\./, '')
    const expanded = suffix ? `${expandedPrefix}.${suffix}` : expandedPrefix
    return expandImportExpression(expanded)
  })
}

function expandImport(statement) {
  const importBody = statement.replace(/^import\s+/, '').trim()
  return splitTopLevel(importBody, ',').flatMap(expandImportExpression)
}

function targetDomain(importPath) {
  return importPath.match(/^domains\.([A-Za-z0-9_]+)(?:\.|$)/)?.[1] ?? null
}

function importedType(importPath) {
  const parts = importPath.split('.')
  const last = parts.at(-1)
  if (!last || last === '*' || /^[a-z]/.test(last)) {
    return null
  }
  return last
}

function isPublicModelImport(domain, importPath) {
  if (!importPath.startsWith(`domains.${domain}.objects.`)) {
    return false
  }

  const typeName = importedType(importPath)
  return typeName !== null && (publicModelTypes.get(domain)?.has(typeName) ?? false)
}

function matchesPublicPath(importPath, publicPath) {
  return importPath === publicPath || importPath.startsWith(`${publicPath}.`)
}

function isPublicWiringImport(domain, importPath) {
  const routerPattern = new RegExp(`^domains\\.${domain}\\.routes\\.[A-Za-z0-9_]*Router(?:\\.|$)`)
  return (
    [...publicWiringBoundaries].some((publicPath) => matchesPublicPath(importPath, publicPath)) ||
    routerPattern.test(importPath)
  )
}

function isExplicitCollaborationImport(importPath) {
  return [...explicitCollaborationBoundaries].some((publicPath) => matchesPublicPath(importPath, publicPath))
}

function isNamedApiObjectImport(domain, importPath) {
  return new RegExp(`^domains\\.${domain}\\.api\\.[A-Z][A-Za-z0-9_]*(?:\\.|$)`).test(importPath)
}

function isPublicBoundaryImport(domain, importPath) {
  return (
    isPublicModelImport(domain, importPath) ||
    isPublicWiringImport(domain, importPath) ||
    isExplicitCollaborationImport(importPath) ||
    isNamedApiObjectImport(domain, importPath)
  )
}

function forbiddenReason(importPath) {
  if (/^domains\.[^.]+\.table(?:\.|$)/.test(importPath)) {
    return 'imports another domain table layer'
  }

  if (/^domains\.[^.]+\.api(?:\.|$)/.test(importPath)) {
    if (/^domains\.[^.]+\.api\.\*$/.test(importPath)) {
      return 'imports another domain API wildcard'
    }
    return 'imports another domain API implementation'
  }

  if (/^domains\.[^.]+\.routes(?:\.|$)/.test(importPath)) {
    return 'imports another domain route implementation'
  }

  return 'is not an allowed public domain boundary'
}

function checkFile(filePath, errors) {
  const fromDomain = sourceDomain(filePath)
  if (fromDomain === null) {
    return
  }

  for (const entry of collectScalaImports(read(filePath))) {
    for (const importPath of expandImport(entry.statement)) {
      const toDomain = targetDomain(importPath)
      if (toDomain === null || toDomain === fromDomain) {
        continue
      }

      if (isPublicBoundaryImport(toDomain, importPath)) {
        continue
      }

      errors.push(`${filePath}:${entry.line} ${forbiddenReason(importPath)}: ${importPath}`)
    }
  }
}

function checkSharedFile(filePath, errors) {
  for (const entry of collectScalaImports(read(filePath))) {
    for (const importPath of expandImport(entry.statement)) {
      if (targetDomain(importPath) !== null) {
        errors.push(`${filePath}:${entry.line} shared backend imports domain package: ${importPath}`)
      }
    }
  }
}

function run() {
  const errors = []
  const files = checkedRoots.flatMap((rootPath) => walk(rootPath, '.scala'))
  const sharedFiles = checkedSharedRoots.flatMap((rootPath) => walk(rootPath, '.scala'))

  for (const filePath of files) {
    checkFile(filePath, errors)
  }

  for (const filePath of sharedFiles) {
    checkSharedFile(filePath, errors)
  }

  if (errors.length > 0) {
    console.error(`Domain boundary check failed with ${errors.length} issue(s):`)
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exit(1)
  }

  console.log('Domain boundary check passed.')
}

run()
