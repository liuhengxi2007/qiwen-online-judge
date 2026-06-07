import type { RatingValue } from '@/objects/rating/RatingValue'
import type { Username } from '@/objects/user/Username'
import type { APIWithSessionMessage } from '@/system/api/api-message'

export class GetUserRating implements APIWithSessionMessage<RatingValue> {
  declare readonly responseType?: RatingValue
  readonly method = 'POST'
  readonly apiPath: string
  private readonly username: Username

  constructor(username: Username) {
    this.username = username
    this.apiPath = 'internal/ratings/user'
  }

  body(): Username {
    return this.username
  }
}
