package domains.user.objects.response

import domains.user.objects.*

import domains.auth.objects.EmailAddress
import domains.user.objects.{DisplayName, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ManagedUserListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean
)

object ManagedUserListItem:
  given Encoder[ManagedUserListItem] = deriveEncoder[ManagedUserListItem]
  given Decoder[ManagedUserListItem] = deriveDecoder[ManagedUserListItem]
