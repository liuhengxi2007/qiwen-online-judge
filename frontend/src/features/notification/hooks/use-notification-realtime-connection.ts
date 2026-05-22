import { useEffect } from 'react'

import { useAuthStore } from '@/features/auth/stores/use-auth-store'
import { notificationEventsUrl } from '@/features/notification/api/notification-client'
import { useNotificationStore } from '@/features/notification/stores/use-notification-store'

let eventSource: EventSource | null = null
let subscriberCount = 0

function ensureEventSource() {
  if (eventSource) {
    return
  }

  eventSource = new EventSource(notificationEventsUrl(), { withCredentials: true })
  eventSource.addEventListener('notifications_changed', () => {
    const store = useNotificationStore.getState()
    void store.refreshUnreadCount()
    if (store.hasLoadedList) {
      void store.refreshNotifications()
    }
  })
}

function releaseEventSource() {
  if (subscriberCount <= 0 && eventSource) {
    eventSource.close()
    eventSource = null
  }
}

export function useNotificationRealtimeConnection() {
  const session = useAuthStore((state) => state.session)
  const refreshUnreadCount = useNotificationStore((state) => state.refreshUnreadCount)
  const hasLoadedUnreadCount = useNotificationStore((state) => state.hasLoadedUnreadCount)
  const clear = useNotificationStore((state) => state.clear)

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
    ensureEventSource()
    if (isFirstSubscriber && !hasLoadedUnreadCount) {
      void refreshUnreadCount()
    }

    return () => {
      subscriberCount -= 1
      releaseEventSource()
    }
  }, [clear, hasLoadedUnreadCount, refreshUnreadCount, session])
}
