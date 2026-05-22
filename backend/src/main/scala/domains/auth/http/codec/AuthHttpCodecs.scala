package domains.auth.http.codec

import domains.auth.application.input.*
import domains.auth.application.output.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object AuthHttpCodecs:
  given Encoder[LoginRequest] = deriveEncoder[LoginRequest]
  given Decoder[LoginRequest] = deriveDecoder[LoginRequest]
  given Encoder[RegisterRequest] = deriveEncoder[RegisterRequest]
  given Decoder[RegisterRequest] = deriveDecoder[RegisterRequest]

  given Encoder[SessionResponse] = deriveEncoder[SessionResponse]
  given Decoder[SessionResponse] = deriveDecoder[SessionResponse]
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]
  given Encoder[RegisterResponse] = deriveEncoder[RegisterResponse]
  given Decoder[RegisterResponse] = deriveDecoder[RegisterResponse]
