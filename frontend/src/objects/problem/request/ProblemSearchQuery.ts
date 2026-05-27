export type ProblemSearchQuery = string & { readonly __brand: 'ProblemSearchQuery' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemSearchQuery(value: string): ProblemSearchQuery {
  return value as ProblemSearchQuery
}

export function problemSearchQueryValue(query: ProblemSearchQuery): string {
  return query
}

export function parseProblemSearchQuery(rawQuery: string): ParseResult<ProblemSearchQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem search query is required.' }
  }
  return { ok: true, value: createProblemSearchQuery(normalized) }
}
