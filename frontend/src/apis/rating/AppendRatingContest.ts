import type { AppendRatingContestRequest } from '@/objects/rating/request/AppendRatingContestRequest'
import type { RatingManageState } from '@/objects/rating/response/RatingManageState'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 向 rating 管理队列追加比赛；输入比赛 slug 和参数，输出新的管理状态。 */
export class AppendRatingContest implements APIWithSessionMessage<RatingManageState> {
  declare readonly responseType?: RatingManageState
  readonly method = 'POST'
  readonly apiPath = 'ratings/manage/contests'
  private readonly request: AppendRatingContestRequest

  constructor(request: AppendRatingContestRequest) {
    this.request = request
  }

  body(): AppendRatingContestRequest {
    return this.request
  }
}
