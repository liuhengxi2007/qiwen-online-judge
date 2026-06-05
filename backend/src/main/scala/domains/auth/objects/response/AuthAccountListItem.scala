package domains.auth.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
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
  problemManager: Boolean,
  contestManager: Boolean
)

object AuthAccountListItem:
  given Encoder[AuthAccountListItem] = deriveEncoder[AuthAccountListItem]
  given Decoder[AuthAccountListItem] = deriveDecoder[AuthAccountListItem]

  def fromParts(account: AuthAccount, profile: UserProfileSettings): AuthAccountListItem =
    val permissions = AuthPermissionFlags.normalize(account.siteManager, account.problemManager, account.contestManager)
    AuthAccountListItem(
      username = account.username,
      displayName = profile.displayName,
      email = account.email,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager
    )
