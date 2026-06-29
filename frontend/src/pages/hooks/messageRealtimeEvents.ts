import { parseDisplayName } from '@/objects/user/DisplayName'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { parseMessageConversationId } from '@/objects/message/MessageConversationId'
import { parseMessageContent } from '@/objects/message/MessageContent'
import type { MessageId } from '@/objects/message/MessageId'
import { parseMessageId } from '@/objects/message/MessageId'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { parseUsername } from '@/objects/user/Username'
import { refreshMessageInbox } from '@/pages/hooks/realtimeRefresh'

/**
 * 会话页使用的浏览器全局消息流事件名。
 */
export const messageStreamEventName = 'qiwen:message-stream-event'

export type MessageRealtimeEventName = 'message_received' | 'conversation_read' | 'inbox_changed'

/**
 * 已解码的消息流事件详情，供需要载荷级更新的消息页面使用。
 */
export type MessageStreamEventDetail =
  | { type: 'message_received'; payload: DirectMessage }
  | { type: 'conversation_read'; payload: ConversationReadStreamPayload }
  | { type: 'inbox_changed'; payload: Record<string, never> }

type ConversationReadStreamPayload = {
  conversationId: MessageConversationId
  readUpToMessageId: MessageId
  readerUsername: Username
}

function dispatchMessageStreamEvent(detail: MessageStreamEventDetail) {
  window.dispatchEvent(new CustomEvent<MessageStreamEventDetail>(messageStreamEventName, { detail }))
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

function requireParsed<T>(result: { ok: true; value: T } | { ok: false; error: string }, field: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${field}: ${result.error}`)
  }

  return result.value
}

function readUserIdentity(value: unknown, field: string): UserIdentity {
  if (!isRecord(value)) {
    throw new Error(`Invalid ${field}.`)
  }

  return {
    username: requireParsed(parseUsername(readString(value.username, `${field} username`)), `${field} username`),
    displayName: requireParsed(parseDisplayName(readString(value.displayName, `${field} display name`)), `${field} display name`),
  }
}

function decodeDirectMessage(value: unknown): DirectMessage {
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

function decodeConversationReadStreamPayload(value: unknown): ConversationReadStreamPayload {
  if (!isRecord(value)) {
    throw new Error('Invalid conversation read event payload.')
  }

  return {
    conversationId: requireParsed(
      parseMessageConversationId(readString(value.conversationId, 'conversation read conversation id')),
      'conversation read conversation id',
    ),
    readUpToMessageId: requireParsed(
      parseMessageId(readString(value.readUpToMessageId, 'conversation read message id')),
      'conversation read message id',
    ),
    readerUsername: requireParsed(parseUsername(readString(value.readerUsername, 'conversation read reader username')), 'conversation read reader username'),
  }
}

function decodeInboxChangedStreamPayload(value: unknown): Record<string, never> {
  if (!isRecord(value)) {
    throw new Error('Invalid inbox changed event payload.')
  }

  return {}
}

function decodeMessageStreamEvent(type: MessageRealtimeEventName, rawData: string): MessageStreamEventDetail | null {
  try {
    const parsed = JSON.parse(rawData) as unknown
    switch (type) {
      case 'message_received':
        return { type, payload: decodeDirectMessage(parsed) }
      case 'conversation_read':
        return { type, payload: decodeConversationReadStreamPayload(parsed) }
      case 'inbox_changed':
        return { type, payload: decodeInboxChangedStreamPayload(parsed) }
    }
  } catch (error) {
    console.error(`Failed to decode ${type} event.`, error)
    return null
  }
}

/** 处理单条消息实时事件，并分发给消息领域订阅者。 */
export function handleMessageRealtimeEvent(type: MessageRealtimeEventName, rawData: string): boolean {
  const decoded = decodeMessageStreamEvent(type, rawData)
  if (!decoded) {
    return false
  }

  void refreshMessageInbox()
  dispatchMessageStreamEvent(decoded)
  return true
}
