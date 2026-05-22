package shared.http.response

import shared.model.*

import shared.model.ApiMessageParams
import shared.http.codec.SharedHttpCodecs.given
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  given Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
