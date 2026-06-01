import type { ProblemReference } from '@/objects/problem/ProblemReference'
import { fromProblemReferenceContract } from '@/objects/problem/ProblemReference'
import { readNullable, readRecord } from '@/objects/shared/PageResponse'

export type ResolveProblemReferenceResponse = {
  problem: ProblemReference | null
}

export function fromResolveProblemReferenceResponseContract(
  value: unknown,
  label = 'resolve problem reference response',
): ResolveProblemReferenceResponse {
  const response = readRecord(value, label)
  return {
    problem: readNullable(response.problem, `${label} problem`, fromProblemReferenceContract),
  }
}
