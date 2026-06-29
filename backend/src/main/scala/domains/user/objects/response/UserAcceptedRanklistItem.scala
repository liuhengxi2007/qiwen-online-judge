package domains.user.objects.response

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** AC 题数排行榜条目，包含用户身份和通过题目数量。 */
final case class UserAcceptedRanklistItem(
  user: UserIdentity,
  acceptedCount: Int
)

/** 提供 AC 题数排行榜条目 JSON 编解码。 */
object UserAcceptedRanklistItem:
  given Encoder[UserAcceptedRanklistItem] = deriveEncoder[UserAcceptedRanklistItem]
  given Decoder[UserAcceptedRanklistItem] = deriveDecoder[UserAcceptedRanklistItem]
