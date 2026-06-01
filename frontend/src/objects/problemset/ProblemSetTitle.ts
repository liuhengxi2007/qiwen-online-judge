export type ProblemSetTitle = string & { readonly __brand: 'ProblemSetTitle' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemSetTitle(value: string): ProblemSetTitle {
  return value as ProblemSetTitle
}

export function problemSetTitleValue(title: ProblemSetTitle): string {
  return title
}

export function parseProblemSetTitle(rawTitle: string): ParseResult<ProblemSetTitle> {
  const normalized = rawTitle.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem set title is required.' }
  }
  if (normalized.length > 120) {
    return { ok: false, error: 'Problem set title must be at most 120 characters.' }
  }
  return { ok: true, value: createProblemSetTitle(normalized) }
}