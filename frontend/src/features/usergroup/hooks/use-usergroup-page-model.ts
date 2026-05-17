import { useEffect, useReducer } from 'react'

import { listUserGroups } from '@/features/usergroup/api/usergroup-client'
import {
  initialUserGroupPageState,
  reduceUserGroupPageState,
} from '@/features/usergroup/domain/usergroup-page-state'
import { translateMessage } from '@/shared/i18n/messages'
import type { PageRequest } from '@/shared/model/Pagination'

export function useUserGroupPageModel(pageRequest: PageRequest) {
  const [state, dispatch] = useReducer(reduceUserGroupPageState, initialUserGroupPageState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void listUserGroups(pageRequest)
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
  }, [pageRequest.page, pageRequest.pageSize])

  return state
}
