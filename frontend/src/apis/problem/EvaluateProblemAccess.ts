import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemAccessEvaluationResponse } from '@/objects/problem/response/ProblemAccessEvaluationResponse'

type EvaluateProblemAccessBody = {
  slug: ProblemSlug
}

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
