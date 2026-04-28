package domains.message.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class MessageHistoryResponse(
  conversation: MessageConversationSummary,
  messages: List[DirectMessage],
  hasMore: Boolean
)

object MessageHistoryResponse:
  given Encoder[MessageHistoryResponse] = deriveEncoder[MessageHistoryResponse]
  given Decoder[MessageHistoryResponse] = deriveDecoder[MessageHistoryResponse]
