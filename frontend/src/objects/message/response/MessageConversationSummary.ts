import type { UserIdentity } from '@/objects/user/UserIdentity'
import { fromUserIdentityContract } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { fromMessageConversationIdContract } from '@/objects/message/MessageConversationId'
import { readNullable, readRecord, readSafeInteger, readString } from '@/objects/shared/PageResponse'

export type MessageConversationSummary = {
  id: MessageConversationId
  otherUser: UserIdentity
  lastMessagePreview: string | null
  lastMessageSenderUsername: Username | null
  lastMessageAt: string
  unreadCount: number
}

export function fromMessageConversationSummaryContract(
  value: unknown,
  label: string,
): MessageConversationSummary {
  const conversation = readRecord(value, label)
  return {
    id: fromMessageConversationIdContract(readString(conversation.id, `${label} id`), `${label} id`),
    otherUser: fromUserIdentityContract(conversation.otherUser, `${label} other user`),
    lastMessagePreview: readNullable(conversation.lastMessagePreview, `${label} last message preview`, readString),
    lastMessageSenderUsername: readNullable(
      conversation.lastMessageSenderUsername,
      `${label} last message sender username`,
      (username, usernameLabel) => fromUsernameContract(readString(username, usernameLabel), usernameLabel),
    ),
    lastMessageAt: readString(conversation.lastMessageAt, `${label} last message at`),
    unreadCount: readSafeInteger(conversation.unreadCount, `${label} unread count`),
  }
}
