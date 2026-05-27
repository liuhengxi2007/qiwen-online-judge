package domains.message.objects.request

import domains.message.objects.*

final case class MarkConversationReadRequest(
  mode: MarkConversationReadMode,
  messageId: Option[MessageId]
)
