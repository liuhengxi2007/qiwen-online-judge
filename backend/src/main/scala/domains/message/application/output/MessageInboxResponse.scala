package domains.message.application.output

import domains.message.model.*

final case class MessageInboxResponse(
  conversations: List[MessageConversationSummary],
  totalUnreadCount: Int,
  page: Int,
  pageSize: Int,
  totalItems: Long
)
