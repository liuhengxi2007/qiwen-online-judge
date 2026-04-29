import { useEffect } from 'react'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { useMessageStore } from '@/features/message/stores/use-message-store'

export const messageStreamEventName = 'qiwen:message-stream-event'

type MessageStreamEventDetail =
  | { type: 'message_received'; payload: unknown }
  | { type: 'conversation_read'; payload: unknown }
  | { type: 'inbox_changed'; payload: unknown }

let eventSource: EventSource | null = null
let subscriberCount = 0

function dispatchMessageStreamEvent(detail: MessageStreamEventDetail) {
  window.dispatchEvent(new CustomEvent<MessageStreamEventDetail>(messageStreamEventName, { detail }))
}

function ensureEventSource(refreshInbox: () => Promise<void>) {
  if (eventSource) {
    return
  }

  eventSource = new EventSource('/api/messages/events', { withCredentials: true })

  eventSource.addEventListener('message_received', (event) => {
    void refreshInbox()
    dispatchMessageStreamEvent({ type: 'message_received', payload: JSON.parse((event as MessageEvent).data) })
  })

  eventSource.addEventListener('conversation_read', (event) => {
    void refreshInbox()
    dispatchMessageStreamEvent({ type: 'conversation_read', payload: JSON.parse((event as MessageEvent).data) })
  })

  eventSource.addEventListener('inbox_changed', (event) => {
    void refreshInbox()
    dispatchMessageStreamEvent({ type: 'inbox_changed', payload: JSON.parse((event as MessageEvent).data) })
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
