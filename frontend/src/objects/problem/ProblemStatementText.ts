export type ProblemStatementText = string & { readonly __brand: 'ProblemStatementText' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemStatementText(value: string): ProblemStatementText {
  return value as ProblemStatementText
}

export function problemStatementTextValue(statement: ProblemStatementText): string {
  return statement
}

export function parseProblemStatementText(rawStatement: string): ParseResult<ProblemStatementText> {
  const normalized = rawStatement.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem statement is required.' }
  }
  if (normalized.length > 20000) {
    return { ok: false, error: 'Problem statement must be at most 20000 characters.' }
  }
  return { ok: true, value: createProblemStatementText(normalized) }
}

export function fromProblemStatementTextContract(value: string, label: string): ProblemStatementText {
  const result = parseProblemStatementText(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemStatementTextContract(value: ProblemStatementText): string {
  return problemStatementTextValue(value)
}
