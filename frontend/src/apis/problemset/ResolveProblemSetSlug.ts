import type { APIMessage } from '@/system/api/api-message'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { ResolveProblemSetSlugResponse } from '@/objects/problemset/response/ResolveProblemSetSlugResponse'

/** 内部题集 slug 解析请求体；用于创建表单可用性检查。 */
type ResolveProblemSetSlugBody = {
  slug: ProblemSetSlug
}

/** 检查题集 slug 是否存在；输入候选 slug，输出占用状态。 */
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
