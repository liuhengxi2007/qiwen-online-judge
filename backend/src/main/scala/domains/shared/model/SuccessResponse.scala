package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

type SuccessResponseParams = Map[String, String]

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: SuccessResponseParams
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
