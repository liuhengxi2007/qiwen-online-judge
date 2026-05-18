package domains.shared.model

import domains.shared.model.ApiMessageParams
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object SuccessResponse:
  given Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
  given Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]
