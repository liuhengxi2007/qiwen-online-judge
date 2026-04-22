package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

type ErrorResponseParams = Map[String, String]

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object ErrorResponse:
  def apply(apiMessage: ApiMessage): ErrorResponse =
    ErrorResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)

  def apply(message: String): ErrorResponse =
    legacy(message)

  def legacy(message: String): ErrorResponse =
    ApiMessageCatalog.legacyError(message).map(apply).getOrElse(ErrorResponse(code = None, message = Some(message), params = Map.empty))

  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  given Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
