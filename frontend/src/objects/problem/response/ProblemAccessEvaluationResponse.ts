import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import { fromProblemDetailContract } from '@/objects/problem/response/ProblemDetail'
import { readBoolean, readNullable, readRecord } from '@/objects/shared/PageResponse'

export type ProblemAccessEvaluationResponse = {
  problem: ProblemDetail | null
  canView: boolean
  canManage: boolean
}

export function fromProblemAccessEvaluationResponseContract(
  value: unknown,
  label = 'problem access evaluation response',
): ProblemAccessEvaluationResponse {
  const response = readRecord(value, label)
  return {
    problem: readNullable(response.problem, `${label} problem`, fromProblemDetailContract),
    canView: readBoolean(response.canView, `${label} can view`),
    canManage: readBoolean(response.canManage, `${label} can manage`),
  }
}
