export type ProblemSlug = string & { readonly __brand: 'ProblemSlug' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function createProblemSlug(value: string): ProblemSlug {
  return value as ProblemSlug
}

export function problemSlugValue(slug: ProblemSlug): string {
  return slug
}

export function parseProblemSlug(rawSlug: string): ParseResult<ProblemSlug> {
  const normalized = rawSlug.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem slug is required.' }
  }
  if (normalized.length < 3 || normalized.length > 64) {
    return { ok: false, error: 'Problem slug must be between 3 and 64 characters.' }
  }
  if (!slugPattern.test(normalized)) {
    return { ok: false, error: 'Problem slug may contain only lowercase letters, numbers, and hyphens.' }
  }
  return { ok: true, value: createProblemSlug(normalized) }
}

export function fromProblemSlugContract(value: string, label: string): ProblemSlug {
  const result = parseProblemSlug(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemSlugContract(value: ProblemSlug): string {
  return problemSlugValue(value)
}
