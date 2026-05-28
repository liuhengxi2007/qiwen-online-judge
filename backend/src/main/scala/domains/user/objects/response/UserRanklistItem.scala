package domains.user.objects.response

import domains.user.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserRanklistItem(
  user: UserIdentity,
  contribution: UserContribution
)

object UserRanklistItem:
  given Encoder[UserRanklistItem] = deriveEncoder[UserRanklistItem]
  given Decoder[UserRanklistItem] = deriveDecoder[UserRanklistItem]
