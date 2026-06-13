import { useCallback, useState } from 'react'

import { MarkAllMessagesRead } from '@/apis/message/MarkAllMessagesRead'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

type UseMessageInboxActionsOptions = {
  refreshInbox: (pageRequest?: PageRequest) => Promise<void>
  markAllReadFailedMessage: string
}

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
