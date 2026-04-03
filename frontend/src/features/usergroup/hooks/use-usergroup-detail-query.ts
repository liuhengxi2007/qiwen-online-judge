import { useEffect, useReducer } from 'react'

import { getUserGroup } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'

type UserGroupDetailQueryState = {
  userGroup: UserGroupDetail | null
  isLoading: boolean
  errorMessage: string
}

type UserGroupDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; userGroup: UserGroupDetail }
  | { type: 'replace'; userGroup: UserGroupDetail }
  | { type: 'load_failed'; message: string }

const initialState: UserGroupDetailQueryState = {
  userGroup: null,
  isLoading: true,
  errorMessage: '',
}

function reducer(state: UserGroupDetailQueryState, action: UserGroupDetailQueryAction): UserGroupDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { userGroup: action.userGroup, isLoading: false, errorMessage: '' }
    case 'replace':
      return { userGroup: action.userGroup, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { userGroup: null, isLoading: false, errorMessage: action.message }
  }
}

export function useUserGroupDetailQuery(userGroupSlug: UserGroupSlug) {
  const [state, dispatch] = useReducer(reducer, initialState)

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
