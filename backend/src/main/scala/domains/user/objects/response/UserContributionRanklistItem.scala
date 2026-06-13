package domains.user.objects.response

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 贡献排行榜条目，包含用户身份和贡献值。 */
final case class UserContributionRanklistItem(
  user: UserIdentity,
  contribution: UserContribution
)

/** 提供贡献排行榜条目 JSON 编解码。 */
object UserContributionRanklistItem:
  given Encoder[UserContributionRanklistItem] = deriveEncoder[UserContributionRanklistItem]
  given Decoder[UserContributionRanklistItem] = deriveDecoder[UserContributionRanklistItem]
