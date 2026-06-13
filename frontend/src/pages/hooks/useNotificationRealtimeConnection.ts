import { useEffect } from 'react'

import { SubscribeNotificationEvents } from '@/apis/notification/SubscribeNotificationEvents'
import { useNotificationRefresh } from '@/pages/hooks/useNotificationRefresh'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { useNotificationStore } from '@/pages/stores/notification/UseNotificationStore'

// 注意：通知 SSE 使用模块级单例和订阅计数，避免多个挂载点重复连接同一事件源。
let notificationEventSource: EventSource | null = null
let notificationSubscriberCount = 0

/**
 * 确保通知 SSE 连接已建立；连接收到变更事件后刷新未读数，并按需刷新已加载列表。
 */
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

/**
 * 在订阅者归零后关闭通知 SSE 连接，避免页面切换后保留多余连接。
 */
function releaseNotificationEventSource() {
  if (notificationSubscriberCount <= 0 && notificationEventSource) {
    notificationEventSource.close()
    notificationEventSource = null
  }
}

/**
 * 管理通知实时连接生命周期；登录时订阅，退出时清空 store 并关闭全局 EventSource。
 */
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
