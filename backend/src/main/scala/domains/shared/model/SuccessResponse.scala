package domains.shared.model

import domains.shared.http.{ApiMessage, ApiMessageParams}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object SuccessResponse:
  def apply(apiMessage: ApiMessage): SuccessResponse =
    SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)

  given Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
  given Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]
