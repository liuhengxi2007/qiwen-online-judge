package domains.message.application.output

import domains.message.model.*

final case class MessageHistoryResponse(
  conversation: MessageConversationSummary,
  messages: List[DirectMessage],
  hasMore: Boolean,
  facts: ConversationMessageFacts
)
