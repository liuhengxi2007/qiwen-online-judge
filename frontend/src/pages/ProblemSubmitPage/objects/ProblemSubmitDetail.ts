import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemTitle } from '@/objects/problem/ProblemTitle'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'

/**
 * 提交页头部只需要题目身份字段，不依赖完整题面详情。
 */
export type ProblemSubmitDetail = {
  slug: ProblemSlug
  title: ProblemTitle
}

export function problemSubmitDetailFromProblem(problem: ProblemDetail): ProblemSubmitDetail {
  return {
    slug: problem.slug,
    title: problem.title,
  }
}
