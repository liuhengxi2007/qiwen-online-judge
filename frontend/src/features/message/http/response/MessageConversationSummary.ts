import type { UserIdentity, Username } from '@/features/auth/domain/auth'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'

export type MessageConversationSummary = {
  id: MessageConversationId
  otherUser: UserIdentity
  lastMessagePreview: string | null
  lastMessageSenderUsername: Username | null
  lastMessageAt: string
  unreadCount: number
}
