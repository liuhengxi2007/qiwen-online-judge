package domains.user.objects.response

import domains.auth.objects.AuthUser
import domains.auth.objects.EmailAddress
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

  def fromAuthUser(user: AuthUser): UserSettingsResponse =
    UserSettingsResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = UserPreferences(
        displayMode = user.displayMode,
        locale = user.locale,
        problemTitleDisplayMode = user.problemTitleDisplayMode,
        autoMarkMessageRead = user.autoMarkMessageRead
      ),
      siteManager = user.siteManager,
      problemManager = user.problemManager
    )
