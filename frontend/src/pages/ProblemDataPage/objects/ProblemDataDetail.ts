import type { ProblemData } from '@/objects/problem/ProblemData'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

/**
 * 测试数据页需要的题目详情字段，不携带题面和访问策略编辑数据。
 */
export type ProblemDataDetail = {
  slug: ProblemSlug
  title: ProblemTitle
  data: ProblemData
  ready: boolean
  canManage: boolean
}

export function problemDataDetailFromProblem(problem: ProblemDetail): ProblemDataDetail {
  return {
    slug: problem.slug,
    title: problem.title,
    data: problem.data,
    ready: problem.ready,
    canManage: problem.canManage,
  }
}
