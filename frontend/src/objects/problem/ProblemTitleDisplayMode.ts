export type ProblemTitleDisplayMode = 'title' | 'slug' | 'title_with_slug'

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

export function problemTitleDisplayModeValue(displayMode: ProblemTitleDisplayMode): ProblemTitleDisplayMode {
  return displayMode
}

export function parseProblemTitleDisplayMode(rawDisplayMode: string): ParseResult<ProblemTitleDisplayMode> {
  const normalized = rawDisplayMode.trim()

  switch (normalized) {
    case 'title':
    case 'slug':
    case 'title_with_slug':
      return { ok: true, value: normalized }
    default:
      return {
        ok: false,
        error: 'Problem title display mode must be one of: title, slug, title_with_slug.',
      }
  }
}

export function fromProblemTitleDisplayModeContract(
  value: string,
  label: string,
): ProblemTitleDisplayMode {
  const result = parseProblemTitleDisplayMode(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemTitleDisplayModeContract(
  value: ProblemTitleDisplayMode,
): ProblemTitleDisplayMode {
  return value
}
