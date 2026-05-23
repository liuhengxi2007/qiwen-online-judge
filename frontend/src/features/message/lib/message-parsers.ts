import { usernameValue } from '@/features/user/lib/user-parsers'
import type { ParseResult } from '@/features/user/lib/user-parsers'
import type { Username } from '@/features/user/model/Username'
import type { MessageContent } from '@/features/message/model/MessageContent'
import type { MessageConversationId } from '@/features/message/model/MessageConversationId'
import type { MessageId } from '@/features/message/model/MessageId'

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

export function messageConversationPath(username: Username): string {
  return `/messages/with/${encodeURIComponent(usernameValue(username))}`
}

export function messageIdValue(messageId: MessageId): string {
  return messageId
}

export function messageContentValue(content: MessageContent): string {
  return content
}
