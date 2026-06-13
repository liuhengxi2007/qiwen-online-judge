import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { problemSetSlugValue } from '@/objects/problemset/ProblemSetSlug'
import type { UpdateProblemSetRequest } from '@/objects/problemset/request/UpdateProblemSetRequest'

/** 更新题集；输入题集 slug 和更新请求，输出更新后的题集详情。 */
export class UpdateProblemSet implements APIWithSessionMessage<ProblemSetDetail> {
  declare readonly responseType?: ProblemSetDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: UpdateProblemSetRequest

  constructor(problemSetSlug: ProblemSetSlug, request: UpdateProblemSetRequest) {
    this.apiPath = `problem-sets/${problemSetSlugValue(problemSetSlug)}`
    this.request = request
  }

  body(): UpdateProblemSetRequest {
    return this.request
  }
}
