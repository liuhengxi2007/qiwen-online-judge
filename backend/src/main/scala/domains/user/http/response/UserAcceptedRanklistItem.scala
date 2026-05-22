package domains.user.http.response

import domains.user.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserAcceptedRanklistItem(
  user: UserIdentity,
  acceptedCount: Int
)

object UserAcceptedRanklistItem:
  given Encoder[UserAcceptedRanklistItem] = deriveEncoder[UserAcceptedRanklistItem]
  given Decoder[UserAcceptedRanklistItem] = deriveDecoder[UserAcceptedRanklistItem]
