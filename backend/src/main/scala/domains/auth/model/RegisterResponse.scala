package domains.auth.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RegisterResponse(
  displayName: DisplayName,
  username: Username,
  email: EmailAddress,
  siteManager: Boolean,
  problemManager: Boolean,
  message: String
)

object RegisterResponse:
  given Encoder[RegisterResponse] = deriveEncoder[RegisterResponse]
  given Decoder[RegisterResponse] = deriveDecoder[RegisterResponse]
