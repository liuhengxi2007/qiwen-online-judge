import { useCallback } from 'react'

import { listInbox } from '@/apis/message/ListInbox'
import { useMessageStore } from '@/pages/stores/message/use-message-store'
import { HttpClientError } from '@/system/api/http-client'
import type { PageRequest } from '@/objects/shared/PageRequest'

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
