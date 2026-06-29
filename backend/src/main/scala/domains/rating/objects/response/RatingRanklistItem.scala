package domains.rating.objects.response

import domains.rating.objects.RatingValue
import domains.user.objects.UserIdentity
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 评分排行榜单行响应，包含用户身份和当前 rating。 */
final case class RatingRanklistItem(
  user: UserIdentity,
  rating: RatingValue
)

/** 提供评分排行榜单行 JSON codec。 */
object RatingRanklistItem:
  given Encoder[RatingRanklistItem] = deriveEncoder[RatingRanklistItem]
  given Decoder[RatingRanklistItem] = deriveDecoder[RatingRanklistItem]
