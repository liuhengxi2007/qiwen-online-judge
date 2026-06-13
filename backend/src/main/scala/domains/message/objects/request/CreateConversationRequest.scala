package domains.message.objects.request


import domains.user.objects.Username
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 创建或获取私信会话的请求体，指定目标用户名。 */
final case class CreateConversationRequest(
  targetUsername: Username
)

/** 提供创建会话请求体 JSON codec。 */
object CreateConversationRequest:
  given Encoder[CreateConversationRequest] = deriveEncoder[CreateConversationRequest]
  given Decoder[CreateConversationRequest] = deriveDecoder[CreateConversationRequest]
