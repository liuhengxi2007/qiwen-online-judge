package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: Map[String, String]
)

object SuccessResponse:
  def apply(message: String): SuccessResponse =
    SuccessResponse(
      code = ApiMessageCatalog.successCodeForMessage(message),
      message = Some(message),
      params = Map.empty
    )

  given Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
  given Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]
