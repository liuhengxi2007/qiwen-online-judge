export type ProblemSetSlug = string & { readonly __brand: 'ProblemSetSlug' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function createProblemSetSlug(value: string): ProblemSetSlug {
  return value as ProblemSetSlug
}

export function problemSetSlugValue(slug: ProblemSetSlug): string {
  return slug
}

export function parseProblemSetSlug(rawSlug: string): ParseResult<ProblemSetSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Problem set slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Problem set slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createProblemSetSlug(normalized) }
}

export function fromProblemSetSlugContract(value: string, label: string): ProblemSetSlug {
  const result = parseProblemSetSlug(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemSetSlugContract(value: ProblemSetSlug): string {
  return problemSetSlugValue(value)
}
