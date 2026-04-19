import { useEffect, useReducer } from 'react'

import { listUserGroups } from '@/features/usergroup/api/usergroup-client'
import {
  initialUserGroupPageState,
  reduceUserGroupPageState,
} from '@/features/usergroup/domain/usergroup-page-state'
import { translateMessage } from '@/shared/i18n/messages'

export function useUserGroupPageModel() {
  const [state, dispatch] = useReducer(reduceUserGroupPageState, initialUserGroupPageState)

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
        dispatch({ type: 'load_failed', message: translateMessage('userGroup.listLoadFailed') })
      })

    return () => {
      cancelled = true
    }
  }, [])

  return state
}
