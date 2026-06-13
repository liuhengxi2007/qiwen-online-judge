import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemAccessEvaluationResponse } from '@/objects/problem/response/ProblemAccessEvaluationResponse'

/** 内部题目访问评估请求体；slug 通过 body 传输以避免直接暴露查询语义。 */
type EvaluateProblemAccessBody = {
  slug: ProblemSlug
}

/** 评估当前会话对题目的访问能力；输出可见题目详情和 view/manage 标志。 */
export class EvaluateProblemAccess implements APIWithSessionMessage<ProblemAccessEvaluationResponse> {
  declare readonly responseType?: ProblemAccessEvaluationResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/problems/evaluate-access'
  private readonly slug: ProblemSlug

  constructor(slug: ProblemSlug) {
    this.slug = slug
  }

  body(): EvaluateProblemAccessBody {
    return { slug: this.slug }
  }
}
