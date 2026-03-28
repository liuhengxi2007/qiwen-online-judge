package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class LoginResponse(displayName: String, email: String, message: String)

object LoginResponse:
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]
