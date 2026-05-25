import { useCallback } from 'react'

import { getNotificationUnreadCount } from '@/features/notification/http/api/GetNotificationUnreadCount'
import { listNotifications } from '@/features/notification/http/api/ListNotifications'
import { useNotificationStore } from '@/features/notification/stores/use-notification-store'
import { HttpClientError } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

const fallbackNotificationLoadError = 'Unable to load notifications.'

export function useNotificationRefresh() {
  const beginNotificationsLoad = useNotificationStore((state) => state.beginNotificationsLoad)
  const replaceNotifications = useNotificationStore((state) => state.replaceNotifications)
  const failNotificationsLoad = useNotificationStore((state) => state.failNotificationsLoad)
  const replaceUnreadCount = useNotificationStore((state) => state.replaceUnreadCount)
  const finishUnreadCountLoad = useNotificationStore((state) => state.finishUnreadCountLoad)

  const refreshNotifications = useCallback(
    async (pageRequest?: PageRequest) => {
      beginNotificationsLoad()
      try {
        replaceNotifications(await listNotifications(pageRequest))
      } catch (error) {
        failNotificationsLoad(error instanceof HttpClientError ? error.message : fallbackNotificationLoadError)
      }
    },
    [beginNotificationsLoad, failNotificationsLoad, replaceNotifications],
  )

  const refreshUnreadCount = useCallback(async () => {
    try {
      const response = await getNotificationUnreadCount()
      replaceUnreadCount(response.unreadCount)
    } catch {
      finishUnreadCountLoad()
    }
  }, [finishUnreadCountLoad, replaceUnreadCount])

  return {
    refreshNotifications,
    refreshUnreadCount,
  }
}
