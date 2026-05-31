package domains.auth.objects.response

import domains.auth.objects.EmailAddress
import domains.user.objects.UserProfileSettings

import domains.user.objects.{DisplayName, UserPreferences, Username}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RegisterResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  preferences: UserPreferences,
  siteManager: Boolean,
  problemManager: Boolean,
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
    message: String
  ): RegisterResponse =
    RegisterResponse(
      displayName = profile.displayName,
      username = profile.username,
      email = email,
      preferences = profile.preferences,
      siteManager = siteManager,
      problemManager = problemManager,
      message = message
    )
