package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class LoginResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean,
  message: String
)

object LoginResponse:
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]
