export type ProblemSetDescription = string & { readonly __brand: 'ProblemSetDescription' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemSetDescription(value: string): ProblemSetDescription {
  return value as ProblemSetDescription
}

export function problemSetDescriptionValue(description: ProblemSetDescription): string {
  return description
}

export function parseProblemSetDescription(rawDescription: string): ParseResult<ProblemSetDescription> {
  const normalized = rawDescription.trim()
  if (normalized.length > 2000) {
    return { ok: false, error: 'Problem set description must be at most 2000 characters.' }
  }
  return { ok: true, value: createProblemSetDescription(normalized) }
}

export function fromProblemSetDescriptionContract(
  value: string,
  label: string,
): ProblemSetDescription {
  const result = parseProblemSetDescription(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemSetDescriptionContract(value: ProblemSetDescription): string {
  return problemSetDescriptionValue(value)
}
