package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

type ErrorResponseParams = Map[String, String]

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ErrorResponseParams
)

object ErrorResponse:
  def apply(message: String): ErrorResponse =
    ErrorResponse(
      code = ApiMessageCatalog.errorCodeForMessage(message),
      message = Some(message),
      params = Map.empty
    )

  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  given Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
