import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { AddProblemToContestRequest } from '@/objects/contest/request/AddProblemToContestRequest'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import { contestSlugValue } from '@/objects/contest/ContestSlug'

/** 向比赛添加题目；输入比赛 slug 和题目请求，输出更新后的比赛详情。 */
export class AddProblemToContest implements APIWithSessionMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'POST'
  readonly apiPath: string
  private readonly request: AddProblemToContestRequest

  constructor(contestSlug: ContestSlug, request: AddProblemToContestRequest) {
    this.apiPath = `contests/${contestSlugValue(contestSlug)}/problems`
    this.request = request
  }

  body(): AddProblemToContestRequest {
    return this.request
  }
}
