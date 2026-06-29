import { useCallback, useState } from 'react'

import { MarkAllMessagesRead } from '@/apis/message/MarkAllMessagesRead'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

/**
 * 收件箱操作 hook 配置，提供刷新回调和全部已读失败兜底文案。
 */
type UseMessageInboxActionsOptions = {
  refreshInbox: (pageRequest?: PageRequest) => Promise<void>
  markAllReadFailedMessage: string
}

/**
 * 私信收件箱操作 hook；提供全部标记已读动作，并在完成后刷新当前分页。
 */
export function useMessageInboxActions({
  refreshInbox,
  markAllReadFailedMessage,
}: UseMessageInboxActionsOptions) {
  const [inboxActionError, setInboxActionError] = useState('')
  const [isMarkingAllRead, setIsMarkingAllRead] = useState(false)

  const markAllRead = useCallback(
    async (pageRequest: PageRequest) => {
      setIsMarkingAllRead(true)
      try {
        await sendAPI(new MarkAllMessagesRead())
        setInboxActionError('')
        await refreshInbox(pageRequest)
      } catch (error) {
        setInboxActionError(isHttpClientError(error) ? error.message : markAllReadFailedMessage)
      } finally {
        setIsMarkingAllRead(false)
      }
    },
    [markAllReadFailedMessage, refreshInbox],
  )

  return {
    inboxActionError,
    isMarkingAllRead,
    markAllRead,
  }
}
