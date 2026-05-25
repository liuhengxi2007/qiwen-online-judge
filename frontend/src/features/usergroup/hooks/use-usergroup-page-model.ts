import { useEffect, useReducer } from 'react'

import { listUserGroups } from '@/features/usergroup/http/api/ListUserGroups'
import {
  initialUserGroupPageState,
  reduceUserGroupPageState,
} from '@/features/usergroup/state/usergroup-page-state'
import { translateMessage } from '@/shared/i18n/messages'
import type { PageRequest } from '@/shared/model/PageRequest'

export function useUserGroupPageModel(pageRequest: PageRequest) {
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [state, dispatch] = useReducer(reduceUserGroupPageState, initialUserGroupPageState)

  useEffect(() => {
    let cancelled = false
    const nextPageRequest = { page, pageSize }
    dispatch({ type: 'load_started' })
    void listUserGroups(nextPageRequest)
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', groups: response.items, page: response.page, pageSize: response.pageSize, totalItems: response.totalItems })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: translateMessage('userGroup.listLoadFailed') })
      })

    return () => {
      cancelled = true
    }
  }, [page, pageSize])

  return state
}
