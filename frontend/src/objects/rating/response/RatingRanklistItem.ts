import type { RatingValue } from '@/objects/rating/RatingValue'
import type { UserIdentity } from '@/objects/user/UserIdentity'

export type RatingRanklistItem = {
  user: UserIdentity
  rating: RatingValue
}
