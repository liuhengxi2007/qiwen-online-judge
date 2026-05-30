import { useEffect, useReducer } from 'react'

import { ListUserGroups } from '@/apis/usergroup/ListUserGroups'
import {
  initialUserGroupPageState,
  reduceUserGroupPageState,
} from '../functions/UserGroupPageState'
import { translateMessage } from '@/system/i18n/messages'
import type { PageRequest } from '@/objects/shared/PageRequest'
import { sendAPI } from '@/system/api/api-message'

export function useUserGroupPageModel(pageRequest: PageRequest) {
  const page = pageRequest.page
  const pageSize = pageRequest.pageSize
  const [state, dispatch] = useReducer(reduceUserGroupPageState, initialUserGroupPageState)

  useEffect(() => {
    let cancelled = false
    const nextPageRequest = { page, pageSize }
    dispatch({ type: 'load_started' })
    void sendAPI(new ListUserGroups(nextPageRequest))
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
