import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

export type ProblemAccessEvaluationResponse = {
  problem: ProblemDetail | null
  canView: boolean
  canManage: boolean
}
