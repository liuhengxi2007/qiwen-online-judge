import { useCallback } from 'react'

import type { PageRequest } from '@/objects/shared/PageRequest'
import { refreshMessageInbox } from '@/pages/hooks/realtimeRefresh'

/**
 * 返回刷新私信收件箱的回调；调用时发起列表请求并把结果或错误写入全局 store。
 */
export function useMessageInboxRefresh() {
  return useCallback(
    async (pageRequest?: PageRequest) => {
      await refreshMessageInbox(pageRequest)
    },
    [],
  )
}
