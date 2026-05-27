import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import type { MessageContent } from '@/objects/message/MessageContent'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import type { MessageId } from '@/objects/message/MessageId'

export type DirectMessage = {
  id: MessageId
  conversationId: MessageConversationId
  sender: UserIdentity
  recipientUsername: Username
  content: MessageContent
  createdAt: string
  readAt: string | null
}
