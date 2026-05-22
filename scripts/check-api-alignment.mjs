import { readdirSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'

const root = process.cwd()

const frontendFeaturesRoot = resolve(root, 'frontend/src/features')
const backendDomainsRoot = resolve(root, 'backend/src/main/scala/domains')

const allowedBackendOnlyDomains = new Set(['judge', 'judger'])
const allowedBackendOnlyEndpoints = new Map([
  ['judger', new Set(['RecordJudgerHeartbeat', 'RegisterJudger'])],
])

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
  return listFilesIfDirectory(join(frontendFeaturesRoot, domain, 'http/api'))
    .map((filePath) => filePath.split('/').at(-1))
    .filter((fileName) => fileName.endsWith('.ts'))
    .filter((fileName) => !fileName.endsWith('-client.ts'))
    .map((fileName) => fileName.replace(/\.ts$/, ''))
    .sort()
}

function backendEndpointNames(domain) {
  return listFilesIfDirectory(join(backendDomainsRoot, domain, 'http/api'))
    .map((filePath) => filePath.split('/').at(-1))
    .filter((fileName) => fileName.endsWith('.scala'))
    .map((fileName) => fileName.replace(/\.scala$/, ''))
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

function run() {
  const errors = []
  const frontendDomains = listDirectories(frontendFeaturesRoot).filter((domain) => frontendEndpointNames(domain).length > 0)
  const backendDomains = listDirectories(backendDomainsRoot).filter((domain) => backendEndpointNames(domain).length > 0)

  const frontendDomainSet = new Set(frontendDomains)
  const backendDomainSet = new Set(backendDomains)

  for (const domain of frontendDomains) {
    if (!backendDomainSet.has(domain)) {
      errors.push(`frontend domain ${domain} has http/api endpoints but no backend http/api domain`)
    }
  }

  for (const domain of backendDomains) {
    if (!frontendDomainSet.has(domain) && !allowedBackendOnlyDomains.has(domain)) {
      errors.push(`backend domain ${domain} has http/api endpoints but no frontend http/api domain`)
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
          `${domain} http/api endpoint mismatch:`,
          `  frontend-only: ${formatList(frontendOnly)}`,
          `  backend-only: ${formatList(backendOnly)}`,
        ].join('\n')
      )
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
