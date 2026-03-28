package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AuthUserListItem(
  username: String,
  displayName: String,
  email: String
)

object AuthUserListItem:
  given Encoder[AuthUserListItem] = deriveEncoder[AuthUserListItem]
  given Decoder[AuthUserListItem] = deriveDecoder[AuthUserListItem]
