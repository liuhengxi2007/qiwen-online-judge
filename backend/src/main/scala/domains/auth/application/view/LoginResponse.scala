package domains.auth.application.view

import domains.auth.model.*

import domains.user.model.UserPreferences
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
