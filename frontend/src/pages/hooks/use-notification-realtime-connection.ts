import { useEffect } from 'react'

import { SubscribeNotificationEvents } from '@/apis/notification/SubscribeNotificationEvents'
import { useNotificationRefresh } from '@/pages/hooks/use-notification-refresh'
import { useAuthStore } from '@/pages/stores/auth/use-auth-store'
import { useNotificationStore } from '@/pages/stores/notification/use-notification-store'

let notificationEventSource: EventSource | null = null
let notificationSubscriberCount = 0

function ensureNotificationEventSource(refreshNotifications: () => Promise<void>, refreshUnreadCount: () => Promise<void>) {
  if (notificationEventSource) {
    return
  }

  notificationEventSource = new EventSource(new SubscribeNotificationEvents().eventUrl(), { withCredentials: true })
  notificationEventSource.addEventListener('notifications_changed', () => {
    const store = useNotificationStore.getState()
    void refreshUnreadCount()
    if (store.hasLoadedList) {
      void refreshNotifications()
    }
  })
}

function releaseNotificationEventSource() {
  if (notificationSubscriberCount <= 0 && notificationEventSource) {
    notificationEventSource.close()
    notificationEventSource = null
  }
}

export function useNotificationRealtimeConnection() {
  const session = useAuthStore((state) => state.session)
  const { refreshNotifications, refreshUnreadCount } = useNotificationRefresh()
  const hasLoadedUnreadCount = useNotificationStore((state) => state.hasLoadedUnreadCount)
  const clear = useNotificationStore((state) => state.clear)

  useEffect(() => {
    if (session) {
      return
    }

    clear()
    if (notificationEventSource) {
      notificationEventSource.close()
      notificationEventSource = null
    }
    notificationSubscriberCount = 0
  }, [clear, session])

  useEffect(() => {
    if (!session) {
      return
    }

    const isFirstSubscriber = notificationSubscriberCount === 0
    notificationSubscriberCount += 1
    ensureNotificationEventSource(refreshNotifications, refreshUnreadCount)
    if (isFirstSubscriber && !hasLoadedUnreadCount) {
      void refreshUnreadCount()
    }

    return () => {
      notificationSubscriberCount -= 1
      releaseNotificationEventSource()
    }
  }, [hasLoadedUnreadCount, refreshNotifications, refreshUnreadCount, session])
}
