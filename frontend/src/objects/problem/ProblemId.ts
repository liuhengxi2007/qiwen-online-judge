export type ProblemId = string & { readonly __brand: 'ProblemId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function createProblemId(value: string): ProblemId {
  return value as ProblemId
}

export function problemIdValue(problemId: ProblemId): string {
  return problemId
}

export function parseProblemId(rawId: string): ParseResult<ProblemId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Problem id must be a valid UUID.' }
  }
  return { ok: true, value: createProblemId(normalized) }
}

export function fromProblemIdContract(value: string, label: string): ProblemId {
  const result = parseProblemId(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}
