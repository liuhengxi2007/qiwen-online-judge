import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'

/** 私信会话摘要；用于收件箱列表展示最近消息和未读数。 */
export type MessageConversationSummary = {
  id: MessageConversationId
  otherUser: UserIdentity
  lastMessagePreview: string | null
  lastMessageSenderUsername: Username | null
  lastMessageAt: string
  unreadCount: number
}
