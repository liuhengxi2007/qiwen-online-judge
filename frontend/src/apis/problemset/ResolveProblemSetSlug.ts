import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ResolveProblemSetSlugResponse } from '@/objects/problemset/response/ResolveProblemSetSlugResponse'

type ResolveProblemSetSlugBody = {
  slug: ProblemSetSlug
}

export class ResolveProblemSetSlug implements APIMessage<ResolveProblemSetSlugResponse> {
  declare readonly responseType?: ResolveProblemSetSlugResponse
  readonly method = 'POST'
  readonly apiPath = 'internal/problem-sets/resolve-slug'
  private readonly slug: ProblemSetSlug

  constructor(slug: ProblemSetSlug) {
    this.slug = slug
  }

  body(): ResolveProblemSetSlugBody {
    return { slug: this.slug }
  }
}
