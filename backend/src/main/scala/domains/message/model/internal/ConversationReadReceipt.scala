package domains.message.model.internal

import domains.message.model.{MessageConversationId, MessageId}
import domains.user.model.Username

final case class ConversationReadReceipt(
  conversationId: MessageConversationId,
  otherParticipant: Username,
  readUpToMessageId: MessageId
)
