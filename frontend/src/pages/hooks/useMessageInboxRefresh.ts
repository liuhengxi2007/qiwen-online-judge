import { useCallback } from 'react'

import { ListInbox } from '@/apis/message/ListInbox'
import { useMessageStore } from '@/pages/stores/message/UseMessageStore'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
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
        replaceInbox(await sendAPI(new ListInbox(pageRequest)))
      } catch (error) {
        failInboxLoad(isHttpClientError(error) ? error.message : fallbackInboxLoadError)
      }
    },
    [beginInboxLoad, failInboxLoad, replaceInbox],
  )
}
