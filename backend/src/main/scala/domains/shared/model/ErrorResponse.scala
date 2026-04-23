package domains.shared.model

import domains.shared.http.{ApiMessage, ApiMessageParams}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object ErrorResponse:
  def apply(apiMessage: ApiMessage): ErrorResponse =
    ErrorResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)

  def apply(message: String): ErrorResponse =
    ErrorResponse(code = None, message = Some(message), params = Map.empty)

  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  given Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
