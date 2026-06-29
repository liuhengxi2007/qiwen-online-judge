import { readFileSync, readdirSync, statSync } from 'node:fs'
import { basename as pathBasename, join, relative, resolve } from 'node:path'

const root = process.cwd()

const frontendApisRoot = resolve(root, 'frontend/src/apis')
const backendDomainsRoot = resolve(root, 'backend/src/main/scala/domains')

const allowedBackendOnlyDomains = new Set(['judge', 'judger'])
const allowedBackendOnlyApiFiles = new Map([
  [
    'auth/ApiObject',
    'Auth-owned API object protocol definitions are backend transport infrastructure, not callable endpoint files.',
  ],
  [
    'auth/ApiObjectRouter',
    'Auth-owned API object router turns backend API objects into http4s routes and has no frontend endpoint wrapper.',
  ],
  [
    'auth/SessionResolver',
    'Auth-owned session resolver is backend cookie/session infrastructure, not a callable endpoint file.',
  ],
  [
    'auth/SessionStore',
    'Auth-owned session store is backend session infrastructure shared by routers and auth endpoints, not a callable endpoint file.',
  ],
  [
    'blog/BlogCommentReplyNotifier',
    'Blog comment reply notification orchestration is backend API support code and is not a frontend endpoint.',
  ],
  [
    'blog/BlogAccessPolicyValidation',
    'Blog access-policy validation is backend API support shared by blog create/update endpoints and is not a frontend endpoint.',
  ],
  [
    'blog/ProblemBlogAccess',
    'Problem-blog authorization and path decoding support is shared by backend blog endpoints and is not a frontend endpoint.',
  ],
  [
    'contest/ContestAccessPolicyValidation',
    'Contest access-policy validation is backend API support shared by contest create/update endpoints and is not a frontend endpoint.',
  ],
  [
    'hack/HackApiSupport',
    'Hack API support owns shared backend workflow helpers used by multiple hack endpoints and is not a frontend endpoint.',
  ],
  [
    'message/MessageEventHub',
    'Message event hub is backend realtime support code and is not a frontend endpoint.',
  ],
  [
    'notification/NotificationEventHub',
    'Notification event hub is backend realtime support code and is not a frontend endpoint.',
  ],
  [
    'notification/NotificationStreamEvent',
    'Notification stream event typing is backend realtime support code and is not a frontend endpoint.',
  ],
  [
    'judger/GetActiveJudgerSupportedLanguages',
    'Judger language discovery is called by worker registration flows and does not have a site frontend API wrapper.',
  ],
  [
    'judger/RecordJudgerHeartbeat',
    'Judger heartbeat is a worker-only endpoint and does not have a site frontend API wrapper.',
  ],
  [
    'judger/RegisterJudger',
    'Judger registration is a worker-only endpoint and does not have a site frontend API wrapper.',
  ],
  [
    'problem/GetProblemSubmissionResultDisplayMode',
    'Submission creation uses this backend-only problem query helper through plan-level collaboration; it is not a frontend endpoint.',
  ],
  [
    'problem/ProblemApiSupport',
    'Problem API support centralizes backend problem loading helpers and is not a frontend endpoint.',
  ],
  [
    'problem/ProblemAccessPolicyValidation',
    'Problem access-policy validation is backend API support shared by problem create/update endpoints and is not a frontend endpoint.',
  ],
  [
    'problem/ProblemDataApiHelpers',
    'Problem data API helpers centralize backend storage workflow helpers used by problem data endpoints and are not a frontend endpoint.',
  ],
  [
    'problem/ProblemDataStorage',
    'Problem data storage is backend storage support shared by judge and problem workflows, not a frontend endpoint.',
  ],
  [
    'problem/ProblemDataStorageConfig',
    'Problem data storage configuration is backend runtime support and is not a frontend endpoint.',
  ],
  [
    'problem/ProblemManagementContext',
    'Problem management context parses and validates backend path/query context shared by problem endpoints and is not a frontend endpoint.',
  ],
  [
    'problemset/ProblemSetAccessPolicyValidation',
    'Problem-set access-policy validation is backend API support shared by problem-set create/update endpoints and is not a frontend endpoint.',
  ],
  [
    'submission/SubmissionJudgeRules',
    'Submission judge rules are backend lifecycle support shared by judge workflows and are not a frontend endpoint.',
  ],
  [
    'submission/SubmissionProgramCleanup',
    'Submission program cleanup is backend storage support used by problem deletion and is not a frontend endpoint.',
  ],
  [
    'submission/SubmissionProgramStorage',
    'Submission program storage is backend storage support shared by submission and judge workflows, not a frontend endpoint.',
  ],
  [
    'submission/SubmissionProgramStorageConfig',
    'Submission program storage configuration is backend runtime support and is not a frontend endpoint.',
  ],
  [
    'user/GetUserAvatar',
    'User avatar bytes are consumed as a raw image URL instead of through a frontend API message wrapper.',
  ],
  [
    'user/UserAvatarApiHelpers',
    'User avatar API support centralizes backend permission and refresh helpers and is not a frontend endpoint.',
  ],
  [
    'usergroup/UserGroupMutationValidation',
    'User-group mutation validation is backend API support shared by user-group mutation endpoints and is not a frontend endpoint.',
  ],
])
const usedBackendOnlyApiFileExceptions = new Set()
const internalOnlyApiTraits = new Set(['InternalOnlyApi', 'InternalOnlyAuthenticatedApi'])

function listDirectories(path) {
  return readdirSync(path)
    .map((name) => ({ name, path: join(path, name) }))
    .filter((entry) => statSync(entry.path).isDirectory())
    .map((entry) => entry.name)
    .sort()
}

function listFilesIfDirectory(path) {
  try {
    if (!statSync(path).isDirectory()) {
      return []
    }
  } catch {
    return []
  }

  return readdirSync(path)
    .map((name) => join(path, name))
    .filter((filePath) => statSync(filePath).isFile())
}

function frontendEndpointNames(domain) {
  return listFilesIfDirectory(join(frontendApisRoot, domain))
    .map((filePath) => pathBasename(filePath))
    .filter((fileName) => fileName.endsWith('.ts'))
    .map((fileName) => fileName.replace(/\.ts$/, ''))
    .sort()
}

function backendApiFileDetails(domain) {
  return listFilesIfDirectory(join(backendDomainsRoot, domain, 'api'))
    .map((filePath) => {
      const fileName = pathBasename(filePath)
      if (!fileName.endsWith('.scala')) {
        return null
      }

      const name = fileName.replace(/\.scala$/, '')
      const source = readFileSync(filePath, 'utf8')
      const apiTrait = source.match(
        new RegExp(`(?:object|final\\s+case\\s+class|final\\s+class)\\s+${name}\\b[\\s\\S]*?extends\\s+([A-Za-z0-9_]*Api)\\b`),
      )?.[1] ?? null

      return {
        name,
        filePath,
        apiTrait,
        apiPath: source.match(/override\s+val\s+path\s*:\s*ApiPath\s*=\s*ApiPath\("([^"]+)"\)/)?.[1] ?? null,
      }
    })
    .filter(Boolean)
    .sort((left, right) => left.name.localeCompare(right.name))
}

function backendApiFileNames(domain) {
  return backendApiFileDetails(domain)
    .map((file) => file.name)
    .sort()
}

function backendEndpointDetails(domain) {
  return backendApiFileDetails(domain).filter((file) => file.apiTrait)
}

function difference(left, right) {
  const rightSet = new Set(right)
  return left.filter((entry) => !rightSet.has(entry))
}

function allowedBackendOnlyKey(domain, name) {
  return `${domain}/${name}`
}

function validateAllowedBackendOnlyApiFiles(errors) {
  for (const [key, reason] of allowedBackendOnlyApiFiles) {
    if (typeof reason !== 'string' || reason.trim().length === 0) {
      errors.push(`${key}: backend-only API file exception must include a non-empty reason`)
    }
  }
}

function useAllowedBackendOnlyApiFile(domain, name) {
  const key = allowedBackendOnlyKey(domain, name)
  if (!allowedBackendOnlyApiFiles.has(key)) {
    return false
  }

  usedBackendOnlyApiFileExceptions.add(key)
  return true
}

function checkUnusedAllowedBackendOnlyApiFiles(errors) {
  for (const key of allowedBackendOnlyApiFiles.keys()) {
    if (!usedBackendOnlyApiFileExceptions.has(key)) {
      errors.push(`${key}: unused backend-only API file exception`)
    }
  }
}

function formatList(entries) {
  return entries.length === 0 ? '(none)' : entries.join(', ')
}

function formatBackendEndpoint(endpoint) {
  return `${relative(root, endpoint.filePath)} (${endpoint.name})`
}

function checkBackendApiPathBoundary(endpoint, errors) {
  if (!endpoint.apiPath) {
    errors.push(`${formatBackendEndpoint(endpoint)} is missing a literal ApiPath declaration`)
    return
  }

  const isInternalOnly = internalOnlyApiTraits.has(endpoint.apiTrait)
  const usesInternalPath = endpoint.apiPath.startsWith('/api/internal/')

  if (isInternalOnly && !usesInternalPath) {
    errors.push(
      `${formatBackendEndpoint(endpoint)} extends ${endpoint.apiTrait} but uses ${endpoint.apiPath}; internal-only APIs must use /api/internal/...`,
    )
  }

  if (!isInternalOnly && usesInternalPath) {
    errors.push(
      `${formatBackendEndpoint(endpoint)} extends ${endpoint.apiTrait} but uses ${endpoint.apiPath}; callable APIs must not use /api/internal/...`,
    )
  }
}

function run() {
  const errors = []
  validateAllowedBackendOnlyApiFiles(errors)
  const frontendDomains = listDirectories(frontendApisRoot).filter((domain) => frontendEndpointNames(domain).length > 0)
  const backendDomains = listDirectories(backendDomainsRoot).filter((domain) => backendApiFileNames(domain).length > 0)

  const frontendDomainSet = new Set(frontendDomains)
  const backendDomainSet = new Set(backendDomains)

  for (const domain of frontendDomains) {
    if (!backendDomainSet.has(domain)) {
      errors.push(`frontend domain ${domain} has api endpoints but no backend api domain`)
    }
  }

  for (const domain of backendDomains) {
    if (!frontendDomainSet.has(domain) && !allowedBackendOnlyDomains.has(domain)) {
      errors.push(`backend domain ${domain} has api endpoints but no frontend api domain`)
    }
  }

  const comparedDomains = frontendDomains.filter((domain) => backendDomainSet.has(domain))
  for (const domain of comparedDomains) {
    const frontendApiFiles = frontendEndpointNames(domain)
    const backendApiFiles = backendApiFileNames(domain)
    const frontendOnly = difference(frontendApiFiles, backendApiFiles)
    const backendOnly = difference(backendApiFiles, frontendApiFiles).filter((apiFile) => !useAllowedBackendOnlyApiFile(domain, apiFile))

    if (frontendOnly.length > 0 || backendOnly.length > 0) {
      errors.push(
        [
          `${domain} api file mismatch:`,
          `  frontend-only: ${formatList(frontendOnly)}`,
          `  backend-only: ${formatList(backendOnly)}`,
        ].join('\n')
      )
    }
  }

  for (const domain of backendDomains) {
    for (const endpoint of backendEndpointDetails(domain)) {
      checkBackendApiPathBoundary(endpoint, errors)
    }
  }

  checkUnusedAllowedBackendOnlyApiFiles(errors)

  if (errors.length > 0) {
    console.error('API alignment check failed:\n')
    for (const error of errors) {
      console.error(`- ${error}`)
    }
    process.exitCode = 1
    return
  }

  console.log('API alignment check passed.')
}

run()
