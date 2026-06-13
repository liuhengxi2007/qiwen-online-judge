import type { APIWithSessionMessage } from '@/system/api/api-message'
import type { CreateContestRequest } from '@/objects/contest/request/CreateContestRequest'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'

/** 创建比赛；输入基础资料、时间窗口和访问策略，输出比赛详情。 */
export class CreateContest implements APIWithSessionMessage<ContestDetail> {
  declare readonly responseType?: ContestDetail
  readonly method = 'POST'
  readonly apiPath = 'contests'
  private readonly request: CreateContestRequest

  constructor(request: CreateContestRequest) {
    this.request = request
  }

  body(): CreateContestRequest {
    return this.request
  }
}
