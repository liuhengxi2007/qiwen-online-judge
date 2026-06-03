import { readFileSync, readdirSync, statSync } from 'node:fs'
import { basename as pathBasename, join, relative, resolve } from 'node:path'

const root = process.cwd()

const frontendApisRoot = resolve(root, 'frontend/src/apis')
const backendDomainsRoot = resolve(root, 'backend/src/main/scala/domains')

const allowedBackendOnlyDomains = new Set(['judge', 'judger'])
const allowedBackendOnlyEndpoints = new Map([
  ['judger', new Set(['GetActiveJudgerSupportedLanguages', 'RecordJudgerHeartbeat', 'RegisterJudger'])],
  ['user', new Set(['GetUserAvatar'])],
])
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

function backendEndpointDetails(domain) {
  return listFilesIfDirectory(join(backendDomainsRoot, domain, 'api'))
    .map((filePath) => {
      const fileName = pathBasename(filePath)
      if (!fileName.endsWith('.scala')) {
        return null
      }
      const basename = fileName.replace(/\.scala$/, '')
      const source = readFileSync(filePath, 'utf8')
      const apiTrait = source.match(
        new RegExp(`(?:object|final\\s+case\\s+class|final\\s+class)\\s+${basename}\\b[\\s\\S]*?extends\\s+([A-Za-z0-9_]*Api)\\b`),
      )?.[1]
      if (!apiTrait) {
        return null
      }

      return {
        name: basename,
        filePath,
        apiTrait,
        apiPath: source.match(/override\s+val\s+path\s*:\s*ApiPath\s*=\s*ApiPath\("([^"]+)"\)/)?.[1] ?? null,
      }
    })
    .filter(Boolean)
    .sort((left, right) => left.name.localeCompare(right.name))
}

function backendEndpointNames(domain) {
  return backendEndpointDetails(domain)
    .map((endpoint) => endpoint.name)
    .sort()
}

function difference(left, right) {
  const rightSet = new Set(right)
  return left.filter((entry) => !rightSet.has(entry))
}

function allowedBackendOnlyNames(domain) {
  return allowedBackendOnlyEndpoints.get(domain) ?? new Set()
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
  const frontendDomains = listDirectories(frontendApisRoot).filter((domain) => frontendEndpointNames(domain).length > 0)
  const backendDomains = listDirectories(backendDomainsRoot).filter((domain) => backendEndpointNames(domain).length > 0)

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
    const frontendEndpoints = frontendEndpointNames(domain)
    const backendEndpoints = backendEndpointNames(domain)
    const frontendOnly = difference(frontendEndpoints, backendEndpoints)
    const allowedBackendOnly = allowedBackendOnlyNames(domain)
    const backendOnly = difference(backendEndpoints, frontendEndpoints).filter((endpoint) => !allowedBackendOnly.has(endpoint))

    if (frontendOnly.length > 0 || backendOnly.length > 0) {
      errors.push(
        [
          `${domain} api endpoint mismatch:`,
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
