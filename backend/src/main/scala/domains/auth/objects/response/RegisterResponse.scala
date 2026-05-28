package domains.auth.objects.response

import domains.auth.objects.*

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

  def fromAuthUser(user: AuthUser, message: String): RegisterResponse =
    RegisterResponse(
      displayName = user.displayName,
      username = user.username,
      email = user.email,
      preferences = SessionResponse.fromAuthUser(user).preferences,
      siteManager = user.siteManager,
      problemManager = user.problemManager,
      message = message
    )
