import { useCallback } from 'react'

import { GetNotificationUnreadCount } from '@/apis/notification/GetNotificationUnreadCount'
import { ListNotifications } from '@/apis/notification/ListNotifications'
import { useNotificationStore } from '@/pages/stores/notification/UseNotificationStore'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
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
        replaceNotifications(await sendAPI(new ListNotifications(pageRequest)))
      } catch (error) {
        failNotificationsLoad(isHttpClientError(error) ? error.message : fallbackNotificationLoadError)
      }
    },
    [beginNotificationsLoad, failNotificationsLoad, replaceNotifications],
  )

  const refreshUnreadCount = useCallback(async () => {
    try {
      const response = await sendAPI(new GetNotificationUnreadCount())
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
