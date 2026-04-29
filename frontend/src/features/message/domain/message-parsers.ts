import { fromUserIdentityContract } from '@/features/auth/domain/auth-contract'
import { parseUsername, usernameValue, type ParseResult } from '@/features/auth/domain/auth'
import { requireParsed } from '@/features/auth/domain/auth-parsers'
import type { CreateConversationRequest } from '@/features/message/model/CreateConversationRequest'
import type { ConversationMessageFacts } from '@/features/message/model/ConversationMessageFacts'
import type { DirectMessage } from '@/features/message/model/DirectMessage'
import type { MarkConversationReadRequest } from '@/features/message/model/MarkConversationReadRequest'
import type { MessageBlockEntry } from '@/features/message/model/MessageBlockEntry'
import type { MessageContent } from '@/features/message/model/MessageContent'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageConversationSummary } from '@/features/message/model/MessageConversationSummary'
import type { MessageHistoryResponse } from '@/features/message/model/MessageHistoryResponse'
import type { MessageId } from '@/features/message/model/MessageId'
import type { MessageInboxResponse } from '@/features/message/model/MessageInboxResponse'
import type { SendDirectMessageRequest } from '@/features/message/model/SendDirectMessageRequest'

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
const maxMessageLength = 5000

function createMessageConversationId(value: string): MessageConversationId {
  return value as MessageConversationId
}

function createMessageId(value: string): MessageId {
  return value as MessageId
}

function createMessageContent(value: string): MessageContent {
  return value as MessageContent
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

export function parseMessageConversationId(rawId: string): ParseResult<MessageConversationId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Conversation id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Conversation id must be a valid UUID.' }
  }

  return { ok: true, value: createMessageConversationId(normalized) }
}

export function parseMessageId(rawId: string): ParseResult<MessageId> {
  const normalized = rawId.trim()
  if (!normalized) {
    return { ok: false, error: 'Message id is required.' }
  }
  if (!uuidPattern.test(normalized)) {
    return { ok: false, error: 'Message id must be a valid UUID.' }
  }

  return { ok: true, value: createMessageId(normalized) }
}

export function parseMessageContent(rawContent: string): ParseResult<MessageContent> {
  const normalized = rawContent.trim()
  if (!normalized) {
    return { ok: false, error: 'Message content is required.' }
  }
  if (normalized.length > maxMessageLength) {
    return { ok: false, error: `Message content must be at most ${maxMessageLength} characters.` }
  }

  return { ok: true, value: createMessageContent(normalized) }
}

export function messageConversationIdValue(conversationId: MessageConversationId): string {
  return conversationId
}

export function messageIdValue(messageId: MessageId): string {
  return messageId
}

export function messageContentValue(content: MessageContent): string {
  return content
}

export function fromMessageConversationSummary(value: unknown): MessageConversationSummary {
  if (!isRecord(value)) {
    throw new Error('Invalid message conversation payload.')
  }

  return {
    id: requireParsed(parseMessageConversationId(readString(value.id, 'conversation id')), 'conversation id'),
    otherUser: readUserIdentity(value.otherUser, 'conversation other user'),
    lastMessagePreview: value.lastMessagePreview === null ? null : readString(value.lastMessagePreview, 'last message preview'),
    lastMessageSenderUsername:
      value.lastMessageSenderUsername === null
        ? null
        : requireParsed(parseUsername(readString(value.lastMessageSenderUsername, 'last message sender username')), 'last message sender username'),
    lastMessageAt: readString(value.lastMessageAt, 'last message at'),
    unreadCount: readNumber(value.unreadCount, 'unread count'),
  }
}

export function fromDirectMessage(value: unknown): DirectMessage {
  if (!isRecord(value)) {
    throw new Error('Invalid direct message payload.')
  }

  return {
    id: requireParsed(parseMessageId(readString(value.id, 'message id')), 'message id'),
    conversationId: requireParsed(parseMessageConversationId(readString(value.conversationId, 'conversation id')), 'conversation id'),
    sender: readUserIdentity(value.sender, 'message sender'),
    recipientUsername: requireParsed(parseUsername(readString(value.recipientUsername, 'recipient username')), 'recipient username'),
    content: requireParsed(parseMessageContent(readString(value.content, 'message content')), 'message content'),
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

export function toCreateConversationRequest(request: CreateConversationRequest): { targetUsername: string } {
  return {
    targetUsername: usernameValue(request.targetUsername),
  }
}

export function toSendDirectMessageRequest(request: SendDirectMessageRequest): { content: string } {
  return {
    content: messageContentValue(request.content),
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
        messageId: messageIdValue(request.messageId),
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
