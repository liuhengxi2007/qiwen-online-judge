import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { MessageContent } from '@/objects/message/MessageContent'
import { fromMessageContentContract } from '@/objects/message/MessageContent'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { fromMessageConversationIdContract } from '@/objects/message/MessageConversationId'
import type { MessageId } from '@/objects/message/MessageId'
import { fromMessageIdContract } from '@/objects/message/MessageId'
import { readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type DirectMessage = {
  id: MessageId
  conversationId: MessageConversationId
  sender: UserIdentity
  recipientUsername: Username
  content: MessageContent
  createdAt: string
  readAt: string | null
}

export function fromDirectMessageContract(value: unknown, label: string): DirectMessage {
  const message = readRecord(value, label)
  return {
    id: fromMessageIdContract(readString(message.id, `${label} id`), `${label} id`),
    conversationId: fromMessageConversationIdContract(
      readString(message.conversationId, `${label} conversation id`),
      `${label} conversation id`,
    ),
    sender: fromUserIdentityContract(message.sender, `${label} sender`),
    recipientUsername: fromUsernameContract(
      readString(message.recipientUsername, `${label} recipient username`),
      `${label} recipient username`,
    ),
    content: fromMessageContentContract(readString(message.content, `${label} content`), `${label} content`),
    createdAt: readString(message.createdAt, `${label} created at`),
    readAt: readNullable(message.readAt, `${label} read at`, readString),
  }
}
