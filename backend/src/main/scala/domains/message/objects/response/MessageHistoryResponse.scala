package domains.message.objects.response


final case class MessageHistoryResponse(
  conversation: MessageConversationSummary,
  messages: List[DirectMessage],
  hasMore: Boolean,
  facts: ConversationMessageFacts
)
