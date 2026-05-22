package domains.message.application.output

import domains.message.model.*

import domains.user.model.Username
import domains.user.model.UserIdentity

import java.time.Instant

final case class MessageConversationSummary(
  id: MessageConversationId,
  otherUser: UserIdentity,
  lastMessagePreview: Option[String],
  lastMessageSenderUsername: Option[Username],
  lastMessageAt: Instant,
  unreadCount: Int
)
