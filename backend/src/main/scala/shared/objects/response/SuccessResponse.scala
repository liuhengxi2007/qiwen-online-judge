package shared.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.api.ApiMessage
import shared.objects.ApiMessageParams

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object SuccessResponse:
  given Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
  given Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]

  def fromApiMessage(apiMessage: ApiMessage): SuccessResponse =
    SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)
