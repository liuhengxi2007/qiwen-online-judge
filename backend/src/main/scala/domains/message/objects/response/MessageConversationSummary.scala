package domains.message.objects.response

import domains.message.objects.*

import domains.user.objects.Username
import domains.user.objects.UserIdentity

import java.time.Instant

final case class MessageConversationSummary(
  id: MessageConversationId,
  otherUser: UserIdentity,
  lastMessagePreview: Option[String],
  lastMessageSenderUsername: Option[Username],
  lastMessageAt: Instant,
  unreadCount: Int
)
