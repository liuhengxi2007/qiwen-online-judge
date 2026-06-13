package domains.message.objects.internal

import domains.message.objects.{MessageConversationId, MessageId}
import domains.user.objects.Username

/** 会话读回执内部对象，记录对端和本次已读推进到的消息 id。 */
final case class ConversationReadReceipt(
  conversationId: MessageConversationId,
  otherParticipant: Username,
  readUpToMessageId: MessageId
)
