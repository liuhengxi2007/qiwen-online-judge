import { useEffect } from 'react'

import {
  handleMessageRealtimeEvent,
  type MessageRealtimeEventName,
} from '@/pages/hooks/messageRealtimeEvents'
import {
  refreshMessageInbox,
  refreshNotificationUnreadCount,
  refreshNotifications,
  refreshRealtimeState,
} from '@/pages/hooks/realtimeRefresh'
import { useRealtimeLeader } from '@/pages/hooks/useRealtimeLeader'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { useMessageStore } from '@/pages/stores/message/UseMessageStore'
import { useNotificationStore } from '@/pages/stores/notification/UseNotificationStore'
import { apiRoutePath } from '@/system/api/api-message'

const realtimeBroadcastChannelName = 'qiwen:realtime-events'

type NotificationRealtimeEventName = 'notifications_changed'
type RealtimeEventName = MessageRealtimeEventName | NotificationRealtimeEventName

type RealtimeBroadcastMessage = {
  eventName: RealtimeEventName
  rawData: string
}

let realtimeEventSource: EventSource | null = null
let realtimeSubscriberCount = 0
let broadcastChannel: BroadcastChannel | null = null
let broadcastListener: ((event: MessageEvent) => void) | null = null

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isRealtimeEventName(value: unknown): value is RealtimeEventName {
  return value === 'message_received' || value === 'conversation_read' || value === 'inbox_changed' || value === 'notifications_changed'
}

function isRealtimeBroadcastMessage(value: unknown): value is RealtimeBroadcastMessage {
  return isRecord(value) && isRealtimeEventName(value.eventName) && typeof value.rawData === 'string'
}

function decodeEmptyPayload(rawData: string): boolean {
  try {
    return isRecord(JSON.parse(rawData) as unknown)
  } catch (error) {
    console.error('Failed to decode notifications_changed event.', error)
    return false
  }
}

function handleNotificationRealtimeEvent(rawData: string): boolean {
  if (!decodeEmptyPayload(rawData)) {
    return false
  }

  const store = useNotificationStore.getState()
  void refreshNotificationUnreadCount()
  if (store.hasLoadedList) {
    void refreshNotifications()
  }
  return true
}

function handleRealtimeEvent(eventName: RealtimeEventName, rawData: string): boolean {
  switch (eventName) {
    case 'message_received':
    case 'conversation_read':
    case 'inbox_changed':
      return handleMessageRealtimeEvent(eventName, rawData)
    case 'notifications_changed':
      return handleNotificationRealtimeEvent(rawData)
  }
}

function ensureBroadcastChannel() {
  if (broadcastChannel || typeof BroadcastChannel === 'undefined') {
    return
  }

  try {
    broadcastChannel = new BroadcastChannel(realtimeBroadcastChannelName)
  } catch {
    broadcastChannel = null
    return
  }

  broadcastListener = (event: MessageEvent) => {
    if (!isRealtimeBroadcastMessage(event.data)) {
      return
    }

    handleRealtimeEvent(event.data.eventName, event.data.rawData)
  }
  broadcastChannel.addEventListener('message', broadcastListener)
}

function releaseBroadcastChannel() {
  if (realtimeSubscriberCount > 0 || !broadcastChannel) {
    return
  }

  if (broadcastListener) {
    broadcastChannel.removeEventListener('message', broadcastListener)
  }
  broadcastChannel.close()
  broadcastChannel = null
  broadcastListener = null
}

function broadcastRealtimeEvent(eventName: RealtimeEventName, rawData: string) {
  broadcastChannel?.postMessage({ eventName, rawData } satisfies RealtimeBroadcastMessage)
}

function addRealtimeEventListener(eventSource: EventSource, eventName: RealtimeEventName) {
  eventSource.addEventListener(eventName, (event) => {
    const rawData = (event as MessageEvent).data
    if (typeof rawData !== 'string') {
      return
    }

    if (handleRealtimeEvent(eventName, rawData)) {
      broadcastRealtimeEvent(eventName, rawData)
    }
  })
}

function ensureRealtimeEventSource() {
  if (realtimeEventSource || typeof EventSource === 'undefined') {
    return
  }

  const eventSource = new EventSource(apiRoutePath('realtime/events'), { withCredentials: true })
  realtimeEventSource = eventSource
  addRealtimeEventListener(eventSource, 'message_received')
  addRealtimeEventListener(eventSource, 'conversation_read')
  addRealtimeEventListener(eventSource, 'inbox_changed')
  addRealtimeEventListener(eventSource, 'notifications_changed')
}

function releaseRealtimeEventSource() {
  if (realtimeEventSource) {
    realtimeEventSource.close()
    realtimeEventSource = null
  }
}

function clearRealtimeState() {
  useMessageStore.getState().clear()
  useNotificationStore.getState().clear()
}

function refreshInitialRealtimeState() {
  const messageStore = useMessageStore.getState()
  const notificationStore = useNotificationStore.getState()
  if (!messageStore.hasLoadedInbox) {
    void refreshMessageInbox()
  }
  if (!notificationStore.hasLoadedUnreadCount) {
    void refreshNotificationUnreadCount()
  }
}

/**
 * 为登录态页面持有合并实时 SSE 传输。
 */
export function useRealtimeConnection() {
  const session = useAuthStore((state) => state.session)
  const isLeader = useRealtimeLeader(Boolean(session))

  useEffect(() => {
    if (!session) {
      clearRealtimeState()
      releaseRealtimeEventSource()
      realtimeSubscriberCount = 0
      releaseBroadcastChannel()
      return
    }

    realtimeSubscriberCount += 1
    ensureBroadcastChannel()
    refreshInitialRealtimeState()

    return () => {
      realtimeSubscriberCount -= 1
      releaseBroadcastChannel()
    }
  }, [session])

  useEffect(() => {
    if (!session || !isLeader) {
      releaseRealtimeEventSource()
      return
    }

    ensureBroadcastChannel()
    ensureRealtimeEventSource()
    void refreshRealtimeState()

    return () => {
      releaseRealtimeEventSource()
    }
  }, [isLeader, session])

  useEffect(() => {
    if (!session) {
      return
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void refreshRealtimeState()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [session])
}
