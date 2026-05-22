import type { UserIdentity, Username } from '@/features/user/domain/user'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'

export type MessageConversationSummary = {
  id: MessageConversationId
  otherUser: UserIdentity
  lastMessagePreview: string | null
  lastMessageSenderUsername: Username | null
  lastMessageAt: string
  unreadCount: number
}
