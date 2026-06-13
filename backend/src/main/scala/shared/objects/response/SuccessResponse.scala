package shared.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import shared.api.ApiMessage
import shared.objects.ApiMessageParams

/** 标准成功响应体，用于无实体业务结果但需要返回消息 code 的接口。 */
final case class SuccessResponse(
  code: Option[String],
  message: Option[String],
  params: ApiMessageParams
)

/** 提供成功响应体编解码，以及从共享 API 消息构造响应的入口。 */
object SuccessResponse:
  given Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
  given Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]

  /** 将内部 ApiMessage 转成客户端响应体，不产生副作用。 */
  def fromApiMessage(apiMessage: ApiMessage): SuccessResponse =
    SuccessResponse(code = Some(apiMessage.code), message = None, params = apiMessage.params)
