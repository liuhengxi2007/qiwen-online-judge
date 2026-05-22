package domains.auth.http.request

import domains.auth.model.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class RegisterRequest(
  username: Username,
  displayName: DisplayName,
  email: EmailAddress,
  password: PlaintextPassword
)

object RegisterRequest:
  given Encoder[RegisterRequest] = deriveEncoder[RegisterRequest]
  given Decoder[RegisterRequest] = deriveDecoder[RegisterRequest]
