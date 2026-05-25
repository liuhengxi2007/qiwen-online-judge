import type { Username } from '@/features/user/model/Username'
import type { CreateConversationRequest } from '@/features/message/http/request/CreateConversationRequest'
import type { ConversationMessageFacts } from '@/features/message/http/response/ConversationMessageFacts'
import type { DirectMessage } from '@/features/message/http/response/DirectMessage'
import type { MarkConversationReadRequest } from '@/features/message/http/request/MarkConversationReadRequest'
import type { MessageBlockEntry } from '@/features/message/http/response/MessageBlockEntry'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageConversationSummary } from '@/features/message/http/response/MessageConversationSummary'
import type { MessageHistoryResponse } from '@/features/message/http/response/MessageHistoryResponse'
import type { MessageId } from '@/features/message/model/MessageId'
import type { MessageInboxResponse } from '@/features/message/http/response/MessageInboxResponse'
import type { SendDirectMessageRequest } from '@/features/message/http/request/SendDirectMessageRequest'
import {
  fromMessageContentContract,
  fromMessageConversationIdContract,
  fromMessageIdContract,
  toMessageContentContract,
  toMessageIdContract,
} from '@/features/message/http/codec/MessageModelHttpCodecs'
import {
  fromUserIdentityContract,
  fromUsernameContract,
  toUsernameContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'

export type ConversationReadStreamPayload = {
  conversationId: MessageConversationId
  readUpToMessageId: MessageId
  readerUsername: Username
}

function fromConversationMessageFacts(value: unknown): ConversationMessageFacts {
  if (!isRecord(value)) {
    throw new Error('Invalid message history facts payload.')
  }

  return {
    viewerHasSentMessage: readBoolean(value.viewerHasSentMessage, 'viewer has sent message'),
    otherParticipantMessageCount: readNumber(value.otherParticipantMessageCount, 'other participant message count'),
  }
}

export function fromMessageConversationSummary(value: unknown): MessageConversationSummary {
  if (!isRecord(value)) {
    throw new Error('Invalid message conversation payload.')
  }

  return {
    id: fromMessageConversationIdContract(readString(value.id, 'conversation id'), 'conversation id'),
    otherUser: readUserIdentity(value.otherUser, 'conversation other user'),
    lastMessagePreview: value.lastMessagePreview === null ? null : readString(value.lastMessagePreview, 'last message preview'),
    lastMessageSenderUsername:
      value.lastMessageSenderUsername === null
        ? null
        : fromUsernameContract(readString(value.lastMessageSenderUsername, 'last message sender username'), 'last message sender username'),
    lastMessageAt: readString(value.lastMessageAt, 'last message at'),
    unreadCount: readNumber(value.unreadCount, 'unread count'),
  }
}

export function fromDirectMessage(value: unknown): DirectMessage {
  if (!isRecord(value)) {
    throw new Error('Invalid direct message payload.')
  }

  return {
    id: fromMessageIdContract(readString(value.id, 'message id'), 'message id'),
    conversationId: fromMessageConversationIdContract(readString(value.conversationId, 'conversation id'), 'conversation id'),
    sender: readUserIdentity(value.sender, 'message sender'),
    recipientUsername: fromUsernameContract(readString(value.recipientUsername, 'recipient username'), 'recipient username'),
    content: fromMessageContentContract(readString(value.content, 'message content'), 'message content'),
    createdAt: readString(value.createdAt, 'message created at'),
    readAt: value.readAt === null ? null : readString(value.readAt, 'message read at'),
  }
}

export function fromMessageHistoryResponse(value: unknown): MessageHistoryResponse {
  if (!isRecord(value) || !Array.isArray(value.messages)) {
    throw new Error('Invalid message history payload.')
  }

  return {
    conversation: fromMessageConversationSummary(value.conversation),
    messages: value.messages.map(fromDirectMessage),
    hasMore: readBoolean(value.hasMore, 'message history has more'),
    facts: fromConversationMessageFacts(value.facts),
  }
}

export function fromMessageInboxResponse(value: unknown): MessageInboxResponse {
  if (!isRecord(value) || !Array.isArray(value.conversations)) {
    throw new Error('Invalid message inbox payload.')
  }

  return {
    conversations: value.conversations.map(fromMessageConversationSummary),
    totalUnreadCount: readNumber(value.totalUnreadCount, 'total unread count'),
    page: readNumber(value.page, 'message inbox page'),
    pageSize: readNumber(value.pageSize, 'message inbox page size'),
    totalItems: readNumber(value.totalItems, 'message inbox total items'),
  }
}

export function fromMessageBlockEntry(value: unknown): MessageBlockEntry {
  if (!isRecord(value)) {
    throw new Error('Invalid message block entry payload.')
  }

  return {
    user: readUserIdentity(value.user, 'blocked user'),
    createdAt: readString(value.createdAt, 'block created at'),
  }
}

export function fromConversationReadStreamPayload(value: unknown): ConversationReadStreamPayload {
  if (!isRecord(value)) {
    throw new Error('Invalid conversation read event payload.')
  }

  return {
    conversationId: fromMessageConversationIdContract(readString(value.conversationId, 'conversation read conversation id'), 'conversation read conversation id'),
    readUpToMessageId: fromMessageIdContract(readString(value.readUpToMessageId, 'conversation read message id'), 'conversation read message id'),
    readerUsername: fromUsernameContract(readString(value.readerUsername, 'conversation read reader username'), 'conversation read reader username'),
  }
}

export function fromInboxChangedStreamPayload(value: unknown): Record<string, never> {
  if (!isRecord(value)) {
    throw new Error('Invalid inbox changed event payload.')
  }

  return {}
}

export function toCreateConversationRequest(request: CreateConversationRequest): { targetUsername: string } {
  return {
    targetUsername: toUsernameContract(request.targetUsername),
  }
}

export function toSendDirectMessageRequest(request: SendDirectMessageRequest): { content: string } {
  return {
    content: toMessageContentContract(request.content),
  }
}

export function toMarkConversationReadRequest(
  request: MarkConversationReadRequest,
): { mode: 'conversation' } | { mode: 'message'; messageId: string } {
  switch (request.mode) {
    case 'conversation':
      return { mode: 'conversation' }
    case 'message':
      return {
        mode: 'message',
        messageId: toMessageIdContract(request.messageId),
      }
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readString(value: unknown, field: string): string {
  if (typeof value !== 'string') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readNumber(value: unknown, field: string): number {
  if (typeof value !== 'number') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readBoolean(value: unknown, field: string): boolean {
  if (typeof value !== 'boolean') {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readRecord(value: unknown, field: string): Record<string, unknown> {
  if (!isRecord(value)) {
    throw new Error(`Invalid ${field}.`)
  }

  return value
}

function readUserIdentity(value: unknown, field: string) {
  const record = readRecord(value, field)
  if (typeof record.username !== 'string' || typeof record.displayName !== 'string') {
    throw new Error(`Invalid ${field}.`)
  }

  return fromUserIdentityContract({
    username: record.username,
    displayName: record.displayName,
  })
}
