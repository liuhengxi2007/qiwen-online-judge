package domains.message.application.output


final case class MessageHistoryResponse(
  conversation: MessageConversationSummary,
  messages: List[DirectMessage],
  hasMore: Boolean,
  facts: ConversationMessageFacts
)
