import type { RatingManageState } from '@/objects/rating/response/RatingManageState'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class GetRatingManageState implements APIWithSessionMessage<RatingManageState> {
  declare readonly responseType?: RatingManageState
  readonly method = 'GET'
  readonly apiPath = 'ratings/manage'

  body(): undefined {
    return undefined
  }
}
