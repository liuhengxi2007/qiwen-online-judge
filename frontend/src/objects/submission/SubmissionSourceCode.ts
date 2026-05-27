export type SubmissionSourceCode = string & { readonly __brand: 'SubmissionSourceCode' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createSubmissionSourceCode(value: string): SubmissionSourceCode {
  return value as SubmissionSourceCode
}

export function submissionSourceCodeValue(sourceCode: SubmissionSourceCode): string {
  return sourceCode
}

export function parseSubmissionSourceCode(rawSourceCode: string): ParseResult<SubmissionSourceCode> {
  if (!rawSourceCode.trim()) {
    return { ok: false, error: 'Source code is required.' }
  }

  if (rawSourceCode.length > 200_000) {
    return { ok: false, error: 'Source code must be at most 200000 characters.' }
  }

  return { ok: true, value: createSubmissionSourceCode(rawSourceCode) }
}

export function fromSubmissionSourceCodeContract(value: string, label: string): SubmissionSourceCode {
  const result = parseSubmissionSourceCode(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toSubmissionSourceCodeContract(value: SubmissionSourceCode): string {
  return submissionSourceCodeValue(value)
}
