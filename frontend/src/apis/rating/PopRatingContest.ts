import type { RatingManageState } from '@/objects/rating/response/RatingManageState'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 从 rating 管理队列移除最后一场比赛；无请求体，输出新的管理状态。 */
export class PopRatingContest implements APIWithSessionMessage<RatingManageState> {
  declare readonly responseType?: RatingManageState
  readonly method = 'POST'
  readonly apiPath = 'ratings/manage/contests/pop'

  body(): undefined {
    return undefined
  }
}
