package domains.message.model

import domains.auth.model.Username

final case class ConversationReadReceipt(
  conversationId: MessageConversationId,
  otherParticipant: Username,
  readUpToMessageId: MessageId
)
