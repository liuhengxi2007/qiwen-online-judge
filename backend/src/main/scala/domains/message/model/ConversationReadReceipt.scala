package domains.message.model



import domains.user.model.Username

final case class ConversationReadReceipt(
  conversationId: MessageConversationId,
  otherParticipant: Username,
  readUpToMessageId: MessageId
)
