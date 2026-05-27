package domains.message.objects.response

import domains.message.objects.*

import domains.user.objects.Username
import domains.user.objects.UserIdentity

import java.time.Instant

final case class DirectMessage(
  id: MessageId,
  conversationId: MessageConversationId,
  sender: UserIdentity,
  recipientUsername: Username,
  content: MessageContent,
  createdAt: Instant,
  readAt: Option[Instant]
)
