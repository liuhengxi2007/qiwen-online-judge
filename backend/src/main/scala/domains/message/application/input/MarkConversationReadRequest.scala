package domains.message.application.input

import domains.message.model.*

enum MarkConversationReadMode:
  case Message
  case Conversation

final case class MarkConversationReadRequest(
  mode: MarkConversationReadMode,
  messageId: Option[MessageId]
)
