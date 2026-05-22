package domains.message.application.output

import domains.message.model.*

import domains.user.model.Username
import domains.user.model.UserIdentity

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
