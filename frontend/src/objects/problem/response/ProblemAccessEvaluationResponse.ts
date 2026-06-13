import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

/** 题目访问评估响应；problem 为空表示不可暴露资源详情或目标不存在。 */
export type ProblemAccessEvaluationResponse = {
  problem: ProblemDetail | null
  canView: boolean
  canManage: boolean
}
