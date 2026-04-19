import { useEffect, useReducer } from 'react'

import { getUserGroup } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import {
  initialUserGroupDetailQueryState,
  reduceUserGroupDetailQueryState,
} from '@/features/usergroup/domain/usergroup-page-state'
import { HttpClientError } from '@/shared/api/http-client'
import { translateMessage } from '@/shared/i18n/messages'

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
