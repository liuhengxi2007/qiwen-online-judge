package domains.auth.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AuthUserListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)

object AuthUserListItem:
  given Encoder[AuthUserListItem] = deriveEncoder[AuthUserListItem]
  given Decoder[AuthUserListItem] = deriveDecoder[AuthUserListItem]
