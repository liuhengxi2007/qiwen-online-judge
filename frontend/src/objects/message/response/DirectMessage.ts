import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import type { MessageContent } from '@/objects/message/MessageContent'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import type { MessageId } from '@/objects/message/MessageId'

/** 私信消息响应；包含发送方、接收方、内容、创建时间和已读时间。 */
export type DirectMessage = {
  id: MessageId
  conversationId: MessageConversationId
  sender: UserIdentity
  recipientUsername: Username
  content: MessageContent
  createdAt: string
  readAt: string | null
}
