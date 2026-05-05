import { useEffect } from 'react'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import {
  fromConversationReadStreamPayload,
  fromDirectMessage,
  fromInboxChangedStreamPayload,
  type ConversationReadStreamPayload,
  type DirectMessage,
} from '@/features/message/domain/message'
import { useMessageStore } from '@/features/message/stores/use-message-store'

export const messageStreamEventName = 'qiwen:message-stream-event'

export type MessageStreamEventDetail =
  | { type: 'message_received'; payload: DirectMessage }
  | { type: 'conversation_read'; payload: ConversationReadStreamPayload }
  | { type: 'inbox_changed'; payload: Record<string, never> }

let eventSource: EventSource | null = null
let subscriberCount = 0

function dispatchMessageStreamEvent(detail: MessageStreamEventDetail) {
  window.dispatchEvent(new CustomEvent<MessageStreamEventDetail>(messageStreamEventName, { detail }))
}

function decodeEventPayload(type: 'message_received' | 'conversation_read' | 'inbox_changed', rawData: string): MessageStreamEventDetail | null {
  try {
    const parsed = JSON.parse(rawData) as unknown
    switch (type) {
      case 'message_received':
        return { type, payload: fromDirectMessage(parsed) }
      case 'conversation_read':
        return { type, payload: fromConversationReadStreamPayload(parsed) }
      case 'inbox_changed':
        return { type, payload: fromInboxChangedStreamPayload(parsed) }
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

  eventSource = new EventSource('/api/messages/events', { withCredentials: true })

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
  const refreshInbox = useMessageStore((state) => state.refreshInbox)
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
