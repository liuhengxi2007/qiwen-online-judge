package domains.message.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 会话消息事实响应，辅助前端判断空会话和双方发言状态。 */
final case class ConversationMessageFacts(
  viewerHasSentMessage: Boolean,
  otherParticipantMessageCount: Int
)

/** 提供会话消息事实 JSON codec。 */
object ConversationMessageFacts:
  given Encoder[ConversationMessageFacts] = deriveEncoder[ConversationMessageFacts]
  given Decoder[ConversationMessageFacts] = deriveDecoder[ConversationMessageFacts]
