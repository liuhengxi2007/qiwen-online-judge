export type ProblemDataFilename = string & { readonly __brand: 'ProblemDataFilename' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemDataFilename(value: string): ProblemDataFilename {
  return value as ProblemDataFilename
}

export function problemDataFilenameValue(filename: ProblemDataFilename): string {
  return filename
}

export function parseProblemDataFilename(rawFilename: string): ParseResult<ProblemDataFilename> {
  const normalized = rawFilename.trim()
  if (!normalized) {
    return { ok: false, error: 'Problem data file name is required.' }
  }
  if (normalized.length > 255) {
    return { ok: false, error: 'Problem data file name must be at most 255 characters.' }
  }
  return { ok: true, value: createProblemDataFilename(normalized) }
}
