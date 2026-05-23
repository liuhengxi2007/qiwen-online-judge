import { useCallback } from 'react'

import { listInbox } from '@/features/message/http/api/message-client'
import { useMessageStore } from '@/features/message/stores/use-message-store'
import { HttpClientError } from '@/shared/api/http-client'
import type { PageRequest } from '@/shared/model/PageRequest'

const fallbackInboxLoadError = 'Unable to load messages.'

export function useMessageInboxRefresh() {
  const beginInboxLoad = useMessageStore((state) => state.beginInboxLoad)
  const replaceInbox = useMessageStore((state) => state.replaceInbox)
  const failInboxLoad = useMessageStore((state) => state.failInboxLoad)

  return useCallback(
    async (pageRequest?: PageRequest) => {
      beginInboxLoad()
      try {
        replaceInbox(await listInbox(pageRequest))
      } catch (error) {
        failInboxLoad(error instanceof HttpClientError ? error.message : fallbackInboxLoadError)
      }
    },
    [beginInboxLoad, failInboxLoad, replaceInbox],
  )
}
