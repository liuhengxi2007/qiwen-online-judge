import { useEffect } from 'react'

import { SubscribeMessageEvents } from '@/apis/message/SubscribeMessageEvents'
import type { MessageConversationId } from '@/objects/message/MessageConversationId'
import { fromMessageConversationIdContract } from '@/objects/message/MessageConversationId'
import { fromMessageContentContract } from '@/objects/message/MessageContent'
import type { MessageId } from '@/objects/message/MessageId'
import { fromMessageIdContract } from '@/objects/message/MessageId'
import type { DirectMessage } from '@/objects/message/response/DirectMessage'
import { useMessageInboxRefresh } from '@/pages/hooks/use-message-inbox-refresh'
import { useAuthStore } from '@/pages/stores/auth/use-auth-store'
import { useMessageStore } from '@/pages/stores/message/use-message-store'
import { fromUserIdentityContract, type UserIdentity } from '@/objects/user/UserIdentity'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'

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

function readUserIdentity(value: unknown, field: string): UserIdentity {
  if (!isRecord(value)) {
    throw new Error(`Invalid ${field}.`)
  }

  return {
    ...fromUserIdentityContract({
      username: readString(value.username, `${field} username`),
      displayName: readString(value.displayName, `${field} display name`),
    }),
  }
}

function decodeDirectMessage(value: unknown): DirectMessage {
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

function decodeConversationReadStreamPayload(value: unknown): ConversationReadStreamPayload {
  if (!isRecord(value)) {
    throw new Error('Invalid conversation read event payload.')
  }

  return {
    conversationId: fromMessageConversationIdContract(
      readString(value.conversationId, 'conversation read conversation id'),
      'conversation read conversation id',
    ),
    readUpToMessageId: fromMessageIdContract(
      readString(value.readUpToMessageId, 'conversation read message id'),
      'conversation read message id',
    ),
    readerUsername: fromUsernameContract(readString(value.readerUsername, 'conversation read reader username'), 'conversation read reader username'),
  }
}

function decodeInboxChangedStreamPayload(value: unknown): Record<string, never> {
  if (!isRecord(value)) {
    throw new Error('Invalid inbox changed event payload.')
  }

  return {}
}

function decodeEventPayload(type: 'message_received' | 'conversation_read' | 'inbox_changed', rawData: string): MessageStreamEventDetail | null {
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
  const decoded = decodeEventPayload(type, (event as MessageEvent).data)
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
