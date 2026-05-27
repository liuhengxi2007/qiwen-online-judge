import type { ProblemDataFilename } from '@/objects/problem/ProblemDataFilename'
import { parseProblemDataFilename } from '@/objects/problem/ProblemDataFilename'

export type ProblemData = {
  value: ProblemDataFilename | null
}

export function fromProblemDataContract(value: string | null, label: string): ProblemData {
  if (value === null) {
    return { value: null }
  }

  const result = parseProblemDataFilename(value)
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return { value: result.value }
}
