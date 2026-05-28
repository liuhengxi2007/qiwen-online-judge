package domains.auth.objects.response

import domains.auth.objects.{AuthUser, EmailAddress}
import domains.user.objects.{DisplayName, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class AuthAccountListItem(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean
)

object AuthAccountListItem:
  given Encoder[AuthAccountListItem] = deriveEncoder[AuthAccountListItem]
  given Decoder[AuthAccountListItem] = deriveDecoder[AuthAccountListItem]

  def fromAuthUser(user: AuthUser): AuthAccountListItem =
    AuthAccountListItem(
      username = user.username,
      displayName = user.displayName,
      email = user.email,
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )
