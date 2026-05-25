import { useCallback, useState } from 'react'

import { markAllMessagesRead } from '@/features/message/http/api/MarkAllMessagesRead'
import { HttpClientError } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

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
        await markAllMessagesRead()
        setInboxActionError('')
        await refreshInbox(pageRequest)
      } catch (error) {
        setInboxActionError(error instanceof HttpClientError ? error.message : markAllReadFailedMessage)
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
