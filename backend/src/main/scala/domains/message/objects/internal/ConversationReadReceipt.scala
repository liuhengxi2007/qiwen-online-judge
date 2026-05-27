package domains.message.objects.internal

import domains.message.objects.{MessageConversationId, MessageId}
import domains.user.objects.Username

final case class ConversationReadReceipt(
  conversationId: MessageConversationId,
  otherParticipant: Username,
  readUpToMessageId: MessageId
)
