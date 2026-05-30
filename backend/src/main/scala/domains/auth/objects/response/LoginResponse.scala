package domains.auth.objects.response

import domains.auth.objects.EmailAddress
import domains.user.objects.internal.UserProfileSettings

import domains.user.objects.{DisplayName, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class LoginResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
  message: String
)

object LoginResponse:
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]

  def fromParts(
    profile: UserProfileSettings,
    email: EmailAddress,
    siteManager: Boolean,
    problemManager: Boolean,
    message: String
  ): LoginResponse =
    LoginResponse(
      displayName = profile.displayName,
      username = profile.username,
      email = email,
      preferences = profile.preferences,
      siteManager = siteManager,
      problemManager = problemManager,
      message = message
    )
