package domains.rating.objects.response

import domains.rating.objects.RatingValue
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RatingRanklistItem(
  user: UserIdentity,
  rating: RatingValue
)

object RatingRanklistItem:
  given Encoder[RatingRanklistItem] = deriveEncoder[RatingRanklistItem]
  given Decoder[RatingRanklistItem] = deriveDecoder[RatingRanklistItem]
