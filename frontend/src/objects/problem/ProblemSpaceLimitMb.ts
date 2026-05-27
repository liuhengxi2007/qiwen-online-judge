export type ProblemSpaceLimitMb = number & { readonly __brand: 'ProblemSpaceLimitMb' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemSpaceLimitMb(value: number): ProblemSpaceLimitMb {
  return value as ProblemSpaceLimitMb
}

export function problemSpaceLimitMbValue(spaceLimitMb: ProblemSpaceLimitMb): number {
  return spaceLimitMb
}

export function parseProblemSpaceLimitMb(rawSpaceLimitMb: number): ParseResult<ProblemSpaceLimitMb> {
  if (!Number.isInteger(rawSpaceLimitMb)) {
    return { ok: false, error: 'Problem space limit must be an integer.' }
  }
  if (rawSpaceLimitMb < 1 || rawSpaceLimitMb > 65536) {
    return { ok: false, error: 'Problem space limit must be between 1 and 65536 MB.' }
  }
  return { ok: true, value: createProblemSpaceLimitMb(rawSpaceLimitMb) }
}

export function fromProblemSpaceLimitMbContract(value: number, label: string): ProblemSpaceLimitMb {
  const result = parseProblemSpaceLimitMb(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemSpaceLimitMbContract(value: ProblemSpaceLimitMb): number {
  return problemSpaceLimitMbValue(value)
}
