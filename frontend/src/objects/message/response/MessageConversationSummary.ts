import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'

export type MessageConversationSummary = {
  id: MessageConversationId
  otherUser: UserIdentity
  lastMessagePreview: string | null
  lastMessageSenderUsername: Username | null
  lastMessageAt: string
  unreadCount: number
}
