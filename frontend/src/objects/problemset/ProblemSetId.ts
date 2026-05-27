export type ProblemSetId = string & { readonly __brand: 'ProblemSetId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createProblemSetId(value: string): ProblemSetId {
  return value as ProblemSetId
}

export function parseProblemSetId(rawId: string): ParseResult<ProblemSetId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Problem set id must be a valid UUID.' }
  }
  return { ok: true, value: createProblemSetId(normalized) }
}

export function fromProblemSetIdContract(value: string, label: string): ProblemSetId {
  const result = parseProblemSetId(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}
