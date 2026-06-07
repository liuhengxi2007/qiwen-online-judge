import type { RatingManageState } from '@/objects/rating/response/RatingManageState'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class PopRatingContest implements APIWithSessionMessage<RatingManageState> {
  declare readonly responseType?: RatingManageState
  readonly method = 'POST'
  readonly apiPath = 'ratings/manage/contests/pop'

  body(): undefined {
    return undefined
  }
}
