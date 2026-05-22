import type { UserIdentity, Username } from '@/features/user/domain/user'
import type { MessageContent } from '@/features/message/model/MessageContent'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageId } from '@/features/message/model/MessageId'

export type DirectMessage = {
  id: MessageId
  conversationId: MessageConversationId
  sender: UserIdentity
  recipientUsername: Username
  content: MessageContent
  createdAt: string
  readAt: string | null
}
