package domains.shared.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

type SuccessResponseParams = Map[String, String]

final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

object SuccessResponse:
  def apply(apiMessage: ApiMessage): SuccessResponse =
    SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)

  def apply(message: String): SuccessResponse =
    legacy(message)

  def legacy(message: String): SuccessResponse =
    ApiMessageCatalog.legacySuccess(message).map(apply).getOrElse(SuccessResponse(code = None, message = Some(message), params = Map.empty))

  given Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
  given Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]
