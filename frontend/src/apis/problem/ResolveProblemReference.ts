import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ResolveProblemReferenceResponse } from '@/objects/problem/response/ResolveProblemReferenceResponse'
import { fromResolveProblemReferenceResponseContract } from '@/objects/problem/response/ResolveProblemReferenceResponse'

type ResolveProblemReferenceBody = {
  slug: ProblemSlug
}

export class ResolveProblemReference implements APIMessage<ResolveProblemReferenceResponse> {
  declare readonly responseType?: ResolveProblemReferenceResponse
  readonly method = 'POST'
  readonly decode = fromResolveProblemReferenceResponseContract
  readonly apiPath = 'internal/problems/resolve-reference'
  private readonly slug: ProblemSlug

  constructor(slug: ProblemSlug) {
    this.slug = slug
  }

  body(): ResolveProblemReferenceBody {
    return { slug: this.slug }
  }
}
