import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { DeleteProblemDataPathRequest } from '@/objects/problem/request/DeleteProblemDataPathRequest'
import type { ProblemDataPath } from '@/objects/problem/ProblemDataPath'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 删除题目数据中的指定路径；输入题目 slug、数据路径和可选比赛上下文。 */
export class DeleteProblemDataPath implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: DeleteProblemDataPathRequest

  constructor(problemSlug: ProblemSlug, path: ProblemDataPath, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/files/delete${params.size > 0 ? `?${params.toString()}` : ''}`
    this.request = { path }
  }

  body(): DeleteProblemDataPathRequest {
    return this.request
  }
}
