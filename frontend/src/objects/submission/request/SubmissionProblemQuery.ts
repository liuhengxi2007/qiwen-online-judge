export type SubmissionProblemQuery = string & { readonly __brand: 'SubmissionProblemQuery' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createSubmissionProblemQuery(value: string): SubmissionProblemQuery {
  return value as SubmissionProblemQuery
}

export function submissionProblemQueryValue(query: SubmissionProblemQuery): string {
  return query
}

export function parseSubmissionProblemQuery(rawQuery: string): ParseResult<SubmissionProblemQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Submission problem query is required.' }
  }
  return { ok: true, value: createSubmissionProblemQuery(normalized) }
}