import type { RatingValue } from '@/objects/rating/RatingValue'
import type { UserIdentity } from '@/objects/user/UserIdentity'

/** Rating 排行榜条目；只包含用户公开身份和 rating 值。 */
export type RatingRanklistItem = {
  user: UserIdentity
  rating: RatingValue
}
