export type ProblemTimeLimitMs = number & { readonly __brand: 'ProblemTimeLimitMs' }

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string }

function createProblemTimeLimitMs(value: number): ProblemTimeLimitMs {
  return value as ProblemTimeLimitMs
}

export function problemTimeLimitMsValue(timeLimitMs: ProblemTimeLimitMs): number {
  return timeLimitMs
}

export function parseProblemTimeLimitMs(rawTimeLimitMs: number): ParseResult<ProblemTimeLimitMs> {
  if (!Number.isInteger(rawTimeLimitMs)) {
    return { ok: false, error: 'Problem time limit must be an integer.' }
  }
  if (rawTimeLimitMs < 1 || rawTimeLimitMs > 600000) {
    return { ok: false, error: 'Problem time limit must be between 1 and 600000 ms.' }
  }
  return { ok: true, value: createProblemTimeLimitMs(rawTimeLimitMs) }
}

export function fromProblemTimeLimitMsContract(value: number, label: string): ProblemTimeLimitMs {
  const result = parseProblemTimeLimitMs(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function toProblemTimeLimitMsContract(value: ProblemTimeLimitMs): number {
  return problemTimeLimitMsValue(value)
}
