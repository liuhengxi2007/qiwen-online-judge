export type SubmissionUserQuery = string & { readonly __brand: 'SubmissionUserQuery' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createSubmissionUserQuery(value: string): SubmissionUserQuery {
  return value as SubmissionUserQuery
}

export function submissionUserQueryValue(query: SubmissionUserQuery): string {
  return query
}

export function parseSubmissionUserQuery(rawQuery: string): ParseResult<SubmissionUserQuery> {
  const normalized = rawQuery.trim()
  if (!normalized) {
    return { ok: false, error: 'Submission username query is required.' }
  }
  return { ok: true, value: createSubmissionUserQuery(normalized) }
}