package shared.objects.transport

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.objects.ApiMessageParams

/** 标准错误响应体，承载可本地化 code、兜底文本和消息参数。 */
final case class ErrorResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

/** 提供错误响应体的 JSON 编解码。 */
object ErrorResponse:
  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  given Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
