import { useCallback } from 'react'

import { getNotificationUnreadCount } from '@/apis/notification/GetNotificationUnreadCount'
import { listNotifications } from '@/apis/notification/ListNotifications'
import { useNotificationStore } from '@/pages/objects/notification/use-notification-store'
import { HttpClientError } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

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
