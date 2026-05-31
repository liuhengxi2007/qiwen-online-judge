package domains.auth.objects.response

import domains.auth.objects.EmailAddress
import domains.auth.objects.internal.AuthAccount
import domains.user.objects.{DisplayName, Username}
import domains.user.objects.UserProfileSettings
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

  def fromParts(account: AuthAccount, profile: UserProfileSettings): AuthAccountListItem =
    AuthAccountListItem(
      username = account.username,
      displayName = profile.displayName,
      email = account.email,
      siteManager = account.siteManager,
      problemManager = account.problemManager
    )
