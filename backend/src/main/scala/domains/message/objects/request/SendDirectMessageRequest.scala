package domains.message.objects.request

import domains.message.objects.*
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 发送私信请求体，携带消息正文。 */
final case class SendDirectMessageRequest(
  content: MessageContent
)

/** 提供发送私信请求体 JSON codec。 */
object SendDirectMessageRequest:
  given Encoder[SendDirectMessageRequest] = deriveEncoder[SendDirectMessageRequest]
  given Decoder[SendDirectMessageRequest] = deriveDecoder[SendDirectMessageRequest]
