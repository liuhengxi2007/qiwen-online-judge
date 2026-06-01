import { useEffect } from 'react'

import { SubscribeMessageEvents } from '@/apis/message/SubscribeMessageEvents'
import { parseDisplayName } from '@/objects/user/DisplayName'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { parseMessageConversationId } from '@/objects/message/MessageConversationId'
import { parseMessageContent } from '@/objects/message/MessageContent'
import type { MessageId } from '@/objects/message/MessageId'
import { parseMessageId } from '@/objects/message/MessageId'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import { useMessageInboxRefresh } from '@/pages/hooks/useMessageInboxRefresh'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { useMessageStore } from '@/pages/stores/message/UseMessageStore'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { parseUsername } from '@/objects/user/Username'

export const messageStreamEventName = 'qiwen:message-stream-event'

export type MessageStreamEventDetail =
  | { type: 'message_received'; payload: DirectMessage }
  | { type: 'conversation_read'; payload: ConversationReadStreamPayload }
  | { type: 'inbox_changed'; payload: Record<string, never> }

type ConversationReadStreamPayload = {
  conversationId: MessageConversationId
  readUpToMessageId: MessageId
  readerUsername: Username
}

let eventSource: EventSource | null = null
let subscriberCount = 0

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

function decodeMessageStreamEvent(type: 'message_received' | 'conversation_read' | 'inbox_changed', rawData: string): MessageStreamEventDetail | null {
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

function handleIncomingEvent(type: 'message_received' | 'conversation_read' | 'inbox_changed', event: Event, refreshInbox: () => Promise<void>) {
  const decoded = decodeMessageStreamEvent(type, (event as MessageEvent).data)
  if (!decoded) {
    return
  }

  void refreshInbox()
  dispatchMessageStreamEvent(decoded)
}

function ensureEventSource(refreshInbox: () => Promise<void>) {
  if (eventSource) {
    return
  }

  eventSource = new EventSource(new SubscribeMessageEvents().eventUrl(), { withCredentials: true })

  eventSource.addEventListener('message_received', (event) => {
    handleIncomingEvent('message_received', event, refreshInbox)
  })

  eventSource.addEventListener('conversation_read', (event) => {
    handleIncomingEvent('conversation_read', event, refreshInbox)
  })

  eventSource.addEventListener('inbox_changed', (event) => {
    handleIncomingEvent('inbox_changed', event, refreshInbox)
  })
}

function releaseEventSource() {
  if (subscriberCount <= 0 && eventSource) {
    eventSource.close()
    eventSource = null
  }
}

export function useMessageRealtimeConnection() {
  const session = useAuthStore((state) => state.session)
  const refreshInbox = useMessageInboxRefresh()
  const hasLoadedInbox = useMessageStore((state) => state.hasLoadedInbox)
  const clear = useMessageStore((state) => state.clear)

  useEffect(() => {
    if (!session) {
      clear()
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
      subscriberCount = 0
      return
    }

    const isFirstSubscriber = subscriberCount === 0
    subscriberCount += 1
    ensureEventSource(refreshInbox)
    if (isFirstSubscriber && !hasLoadedInbox) {
      void refreshInbox()
    }

    return () => {
      subscriberCount -= 1
      releaseEventSource()
    }
  }, [clear, hasLoadedInbox, refreshInbox, session])
}
