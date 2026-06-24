import { useCallback } from 'react'

import type { PageRequest } from '@/objects/shared/PageRequest'
import { refreshNotificationUnreadCount, refreshNotifications } from '@/pages/hooks/realtimeRefresh'

/**
 * 返回刷新通知列表和未读数的回调集合；请求结果写入通知全局 store。
 */
export function useNotificationRefresh() {
  const refreshNotificationList = useCallback(
    async (pageRequest?: PageRequest) => {
      await refreshNotifications(pageRequest)
    },
    [],
  )

  const refreshUnreadCount = useCallback(async () => {
    await refreshNotificationUnreadCount()
  }, [])

  return {
    refreshNotifications: refreshNotificationList,
    refreshUnreadCount,
  }
}
