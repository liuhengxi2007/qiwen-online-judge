import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ResolveProblemReferenceResponse } from '@/objects/problem/response/ResolveProblemReferenceResponse'

/** 内部题目引用解析请求体；用于将 slug 解析为轻量题目引用。 */
type ResolveProblemReferenceBody = {
  slug: ProblemSlug
}

/** 解析题目引用；输入 slug，输出题目引用或空值。 */
export class ResolveProblemReference implements APIMessage<ResolveProblemReferenceResponse> {
  declare readonly responseType?: ResolveProblemReferenceResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/problems/resolve-reference'
  private readonly slug: ProblemSlug

  constructor(slug: ProblemSlug) {
    this.slug = slug
  }

  body(): ResolveProblemReferenceBody {
    return { slug: this.slug }
  }
}
