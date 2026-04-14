import { useEffect, useReducer } from 'react'

import { getUserGroup } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import {
  initialUserGroupDetailQueryState,
  reduceUserGroupDetailQueryState,
} from '@/features/usergroup/domain/usergroup-page-state'

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
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load user group details.' })
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
