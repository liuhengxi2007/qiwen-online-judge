package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: Map[String, String]
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
