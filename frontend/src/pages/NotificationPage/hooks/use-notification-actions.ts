import { useCallback, useState } from 'react'

import { MarkAllNotificationsRead } from '@/apis/notification/MarkAllNotificationsRead'
import { MarkNotificationRead } from '@/apis/notification/MarkNotificationRead'
import type { NotificationId } from '@/objects/notification/NotificationId'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

type UseNotificationActionsOptions = {
  refreshNotifications: (pageRequest?: PageRequest) => Promise<void>
  markReadLocal: (notificationId: NotificationId) => void
  markAllReadLocal: () => void
  markAllReadFailedMessage: string
}

export function useNotificationActions({
  refreshNotifications,
  markReadLocal,
  markAllReadLocal,
  markAllReadFailedMessage,
}: UseNotificationActionsOptions) {
  const [actionError, setActionError] = useState('')
  const [isMarkingAllRead, setIsMarkingAllRead] = useState(false)

  const markReadIfNeeded = useCallback(
    async (notificationId: NotificationId, isRead: boolean) => {
      if (isRead) {
        return
      }

      try {
        await sendAPI(new MarkNotificationRead(notificationId))
        markReadLocal(notificationId)
      } catch {
        // Keep the caller's navigation working even if mark-read fails.
      }
    },
    [markReadLocal],
  )

  const markAllRead = useCallback(
    async (pageRequest: PageRequest) => {
      setIsMarkingAllRead(true)
      try {
        await sendAPI(new MarkAllNotificationsRead())
        setActionError('')
        markAllReadLocal()
        await refreshNotifications(pageRequest)
      } catch (error) {
        setActionError(error instanceof HttpClientError ? error.message : markAllReadFailedMessage)
      } finally {
        setIsMarkingAllRead(false)
      }
    },
    [markAllReadFailedMessage, markAllReadLocal, refreshNotifications],
  )

  return {
    actionError,
    isMarkingAllRead,
    markReadIfNeeded,
    markAllRead,
  }
}
