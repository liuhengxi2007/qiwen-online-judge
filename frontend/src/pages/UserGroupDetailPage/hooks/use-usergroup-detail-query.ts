import { useEffect, useReducer } from 'react'

import { getUserGroup } from '@/apis/usergroup/GetUserGroup'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import {
  initialUserGroupDetailQueryState,
  reduceUserGroupDetailQueryState,
} from '@/pages/objects/usergroup/usergroup-page-state'
import { HttpClientError } from '@/system/api/http-client'
import { translateMessage } from '@/system/i18n/messages'

export function useUserGroupDetailQuery(userGroupSlug: UserGroupSlug) {
  const [state, dispatch] = useReducer(reduceUserGroupDetailQueryState, initialUserGroupDetailQueryState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void getUserGroup(userGroupSlug)
      .then((userGroup) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', userGroup })
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return
        }
        dispatch({
          type: 'load_failed',
          message:
            error instanceof HttpClientError && (error.kind === 'not-found' || error.kind === 'forbidden')
              ? translateMessage('common.error.notFound')
              : translateMessage('userGroup.detailLoadFailed'),
        })
      })

    return () => {
      cancelled = true
    }
  }, [userGroupSlug])

  function replaceUserGroup(userGroup: UserGroupDetail) {
    dispatch({ type: 'replace', userGroup })
  }

  return {
    ...state,
    replaceUserGroup,
  }
}
