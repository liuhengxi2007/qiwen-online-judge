package objects

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class LoginRequest(email: String, password: String)

object LoginRequest:
  given Encoder[LoginRequest] = deriveEncoder[LoginRequest]
  given Decoder[LoginRequest] = deriveDecoder[LoginRequest]
