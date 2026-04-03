import { useEffect, useReducer } from 'react'

import { listUserGroups } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupSummary } from '@/features/usergroup/domain/usergroup'

type UserGroupPageState = {
  groups: UserGroupSummary[]
  isLoading: boolean
  errorMessage: string
}

type UserGroupPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; groups: UserGroupSummary[] }
  | { type: 'load_failed'; message: string }

const initialState: UserGroupPageState = {
  groups: [],
  isLoading: true,
  errorMessage: '',
}

function reducer(state: UserGroupPageState, action: UserGroupPageAction): UserGroupPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { groups: action.groups, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { groups: [], isLoading: false, errorMessage: action.message }
  }
}

export function useUserGroupPageModel() {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void listUserGroups()
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', groups: response.items })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load user groups.' })
      })

    return () => {
      cancelled = true
    }
  }, [])

  return state
}
