package domains.user.objects.response

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserContributionRanklistItem(
  user: UserIdentity,
  contribution: UserContribution
)

object UserContributionRanklistItem:
  given Encoder[UserContributionRanklistItem] = deriveEncoder[UserContributionRanklistItem]
  given Decoder[UserContributionRanklistItem] = deriveDecoder[UserContributionRanklistItem]
