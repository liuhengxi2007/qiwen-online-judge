package domains.message.model.request

import domains.message.model.*

final case class MarkConversationReadRequest(
  mode: MarkConversationReadMode,
  messageId: Option[MessageId]
)
