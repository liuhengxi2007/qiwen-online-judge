package domains.message.application.output


final case class MessageInboxResponse(
  conversations: List[MessageConversationSummary],
  totalUnreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)
