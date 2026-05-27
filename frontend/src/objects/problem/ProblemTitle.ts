export type ProblemTitle = string & { readonly __brand: 'ProblemTitle' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemTitle(value: string): ProblemTitle {
  return value as ProblemTitle
}

export function problemTitleValue(title: ProblemTitle): string {
  return title
}

export function parseProblemTitle(rawTitle: string): ParseResult<ProblemTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Problem title must be at most 120 characters.' }
  }
  return { ok: true, value: createProblemTitle(normalized) }
}

export function fromProblemTitleContract(value: string, label: string): ProblemTitle {
  const result = parseProblemTitle(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemTitleContract(value: ProblemTitle): string {
  return problemTitleValue(value)
}
