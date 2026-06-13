import { useCallback, useState } from 'react'

import { MarkAllNotificationsRead } from '@/apis/notification/MarkAllNotificationsRead'
import { MarkNotificationRead } from '@/apis/notification/MarkNotificationRead'
import type { NotificationId } from '@/objects/notification/NotificationId'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

/**
 * 通知操作 hook 配置，提供刷新回调和失败兜底文案。
 */
type UseNotificationActionsOptions = {
  refreshNotifications: (pageRequest?: PageRequest) => Promise<void>
  markReadLocal: (notificationId: NotificationId) => void
  markAllReadLocal: () => void
  markAllReadFailedMessage: string
}

/**
 * 通知操作 hook；提供单条已读和全部已读动作，并维护操作中状态。
 */
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
        // 注意：单条标记已读失败不阻断通知点击后的导航，后续刷新会重新同步未读状态。
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
        setActionError(isHttpClientError(error) ? error.message : markAllReadFailedMessage)
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
