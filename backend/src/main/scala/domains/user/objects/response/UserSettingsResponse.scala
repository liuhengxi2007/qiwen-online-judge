package domains.user.objects.response

import domains.auth.objects.EmailAddress
import domains.user.objects.internal.UserProfileSettings
import domains.user.objects.{DisplayName, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class UserSettingsResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean
)

object UserSettingsResponse:
  given Encoder[UserSettingsResponse] = deriveEncoder[UserSettingsResponse]
  given Decoder[UserSettingsResponse] = deriveDecoder[UserSettingsResponse]

  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean
  ): UserSettingsResponse =
    UserSettingsResponse(
      displayName = profile.displayName,
      username = profile.username,
      email = email,
      preferences = profile.preferences,
      siteManager = siteManager,
      problemManager = problemManager
    )
