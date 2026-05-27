import { useEffect, useReducer } from 'react'

import { getUserGroup } from '@/apis/usergroup/GetUserGroup'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { HttpClientError } from '@/system/api/http-client'
import { translateMessage } from '@/system/i18n/messages'

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

const initialUserGroupDetailQueryState: UserGroupDetailQueryState = {
  userGroup: null,
  isLoading: true,
  errorMessage: '',
}

function reduceUserGroupDetailQueryState(
  state: UserGroupDetailQueryState,
  action: UserGroupDetailQueryAction,
): UserGroupDetailQueryState {
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
