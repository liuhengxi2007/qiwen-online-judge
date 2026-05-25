import type { UserIdentity } from '@/features/user/model/UserIdentity'
import type { Username } from '@/features/user/model/Username'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'

export type MessageConversationSummary = {
  id: MessageConversationId
  otherUser: UserIdentity
  lastMessagePreview: string | null
  lastMessageSenderUsername: Username | null
  lastMessageAt: string
  unreadCount: number
}
