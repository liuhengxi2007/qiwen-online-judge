package domains.message.objects.response

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** 私信历史响应，包含会话摘要、消息页、更多标记和会话事实。 */
final case class MessageHistoryResponse(
  conversation: MessageConversationSummary,
  messages: List[DirectMessage],
  hasMore: Boolean,
  facts: ConversationMessageFacts
)

/** 提供私信历史响应 JSON codec。 */
object MessageHistoryResponse:
  given Encoder[MessageHistoryResponse] = deriveEncoder[MessageHistoryResponse]
  given Decoder[MessageHistoryResponse] = deriveDecoder[MessageHistoryResponse]
