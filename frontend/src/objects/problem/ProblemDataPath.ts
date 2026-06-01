export type ProblemDataPath = string & { readonly __brand: 'ProblemDataPath' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemDataPath(value: string): ProblemDataPath {
  return value as ProblemDataPath
}

export function problemDataPathValue(path: ProblemDataPath): string {
  return path
}

export function parseProblemDataPath(rawPath: string): ParseResult<ProblemDataPath> {
  const normalized = rawPath.trim().replaceAll('\\', '/')
  if (!normalized) {
    return { ok: false, error: 'Problem data path is required.' }
  }
  if (normalized.length > 1024) {
    return { ok: false, error: 'Problem data path must be at most 1024 characters.' }
  }
  if (normalized.startsWith('/') || normalized.endsWith('/')) {
    return { ok: false, error: "Problem data path must be relative and must not start or end with '/'." }
  }
  const segments = normalized.split('/')
  if (segments.some((segment) => !segment)) {
    return { ok: false, error: 'Problem data path must not contain empty segments.' }
  }
  if (segments.some((segment) => segment === '.' || segment === '..')) {
    return { ok: false, error: "Problem data path must not contain '.' or '..' segments." }
  }
  if (segments.some((segment) => segment.length > 255)) {
    return { ok: false, error: 'Each problem data path segment must be at most 255 characters.' }
  }
  return { ok: true, value: createProblemDataPath(normalized) }
}

export function fromProblemDataPathContract(value: string, label: string): ProblemDataPath {
  const result = parseProblemDataPath(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}
