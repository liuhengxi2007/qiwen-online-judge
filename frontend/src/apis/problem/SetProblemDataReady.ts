import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'

/** 设置题目数据就绪状态的请求体；ready 表示是否允许进入判题使用。 */
type SetProblemDataReadyRequest = {
  ready: boolean
}

/** 设置题目数据就绪状态；输入题目 slug、ready 和可选比赛上下文，输出题目详情。 */
export class SetProblemDataReady implements APIWithSessionMessage<ProblemDetail> {
  declare readonly responseType?: ProblemDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: SetProblemDataReadyRequest

  constructor(problemSlug: ProblemSlug, ready: boolean, contestSlug?: ContestSlug) {
    const params = new URLSearchParams()
    if (contestSlug) {
      params.set('contestSlug', contestSlugValue(contestSlug))
    }
    this.apiPath = `problems/${problemSlugValue(problemSlug)}/data/ready-state${params.size > 0 ? `?${params.toString()}` : ''}`
    this.request = { ready }
  }

  body(): SetProblemDataReadyRequest {
    return this.request
  }
}
