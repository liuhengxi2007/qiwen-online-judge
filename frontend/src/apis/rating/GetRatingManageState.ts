import type { RatingManageState } from '@/objects/rating/response/RatingManageState'
import type { APIWithSessionMessage } from '@/system/api/api-message'

/** 获取 rating 管理状态；无请求体，输出已追加比赛列表。 */
export class GetRatingManageState implements APIWithSessionMessage<RatingManageState> {
  declare readonly responseType?: RatingManageState
  readonly method = 'GET'
  readonly apiPath = 'ratings/manage'

  body(): undefined {
    return undefined
  }
}
