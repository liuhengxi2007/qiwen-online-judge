export type SubmissionId = number & { readonly __brand: 'SubmissionId' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createSubmissionId(value: number): SubmissionId {
  return value as SubmissionId
}

export function submissionIdValue(submissionId: SubmissionId): number {
  return submissionId
}

export function parseSubmissionId(rawId: number): ParseResult<SubmissionId> {
  if (!Number.isSafeInteger(rawId)) {
    return { ok: false, error: 'Submission id must be an integer.' }
  }

  if (rawId < 1) {
    return { ok: false, error: 'Submission id is required.' }
  }

  return { ok: true, value: createSubmissionId(rawId) }
}