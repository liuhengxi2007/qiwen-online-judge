package domains.auth.objects.response

import domains.auth.objects.{AuthPermissionFlags, EmailAddress}
import domains.user.objects.UserProfileSettings

import domains.user.objects.{DisplayName, UserAvatarUrl, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RegisterResponse(
  displayName: DisplayName,
  username: Username,
  avatarUrl: Option[UserAvatarUrl],
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
  contestManager: Boolean,
  message: String
)

object RegisterResponse:
  given Encoder[RegisterResponse] = deriveEncoder[RegisterResponse]
  given Decoder[RegisterResponse] = deriveDecoder[RegisterResponse]

  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    contestManager: Boolean,
    message: String
  ): RegisterResponse =
    val permissions = AuthPermissionFlags.normalize(siteManager, problemManager, contestManager)
    RegisterResponse(
      displayName = profile.displayName,
      username = profile.username,
      avatarUrl = profile.avatarUrl,
      email = email,
      preferences = profile.preferences,
      siteManager = permissions.siteManager,
      problemManager = permissions.problemManager,
      contestManager = permissions.contestManager,
      message = message
    )
