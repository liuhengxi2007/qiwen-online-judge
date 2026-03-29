import { useCallback, useReducer } from 'react'

import { usernameValue, type AuthUserListItem, type UpdateUserPermissionsRequest } from '@/domain/auth'
import { useSiteManageQuery } from '@/hooks/use-site-manage-query'
import { useUserPermissionsMutation } from '@/hooks/use-user-permissions-mutation'
import type { NavigationIntent } from '@/lib/navigation-intent'

type SiteManageState = {
  statusMessage: string
  actionErrorMessage: string
  updatingUsername: string | null
  navigationIntent: NavigationIntent | null
}

type SiteManageAction =
  | { type: 'update_started'; username: string }
  | { type: 'update_succeeded'; user: AuthUserListItem }
  | { type: 'update_failed'; message: string }
  | { type: 'redirect_requested'; intent: NavigationIntent }

const initialState: SiteManageState = {
  statusMessage: '',
  actionErrorMessage: '',
  updatingUsername: null,
  navigationIntent: null,
}

function siteManageReducer(state: SiteManageState, action: SiteManageAction): SiteManageState {
  switch (action.type) {
    case 'update_started':
      return {
        ...state,
        updatingUsername: action.username,
        statusMessage: '',
        actionErrorMessage: '',
      }
    case 'update_succeeded':
      return {
        ...state,
        updatingUsername: null,
        statusMessage: `Permissions updated for ${usernameValue(action.user.username)}.`,
        actionErrorMessage: '',
      }
    case 'update_failed':
      return {
        ...state,
        updatingUsername: null,
        statusMessage: '',
        actionErrorMessage: action.message,
      }
    case 'redirect_requested':
      return {
        ...state,
        navigationIntent: action.intent,
      }
  }
}

export function useSiteManageModel(siteManagerEnabled: boolean) {
  const query = useSiteManageQuery(siteManagerEnabled)
  const [state, dispatch] = useReducer(siteManageReducer, initialState)
  const mutation = useUserPermissionsMutation()
  const currentUpdatingUsername = state.updatingUsername ?? mutation.updatingUsername

  const savePermissions = useCallback(
    async (listedUser: AuthUserListItem, nextPermissions: UpdateUserPermissionsRequest) => {
      if (currentUpdatingUsername) {
        return
      }

      const targetUsername = usernameValue(listedUser.username)
      dispatch({ type: 'update_started', username: targetUsername })

      const result = await mutation.savePermissions(targetUsername, nextPermissions)

      switch (result.kind) {
        case 'updated':
          dispatch({ type: 'update_succeeded', user: result.user })
          return
        case 'forbidden':
          dispatch({ type: 'redirect_requested', intent: { to: '/?notice=site-manage-denied', replace: true } })
          return
        case 'failed':
          dispatch({ type: 'update_failed', message: result.message })
          return
      }
    },
    [currentUpdatingUsername, mutation],
  )

  return {
    users: query.users,
    userListError: state.actionErrorMessage || query.userListError,
    isLoadingUsers: query.isLoadingUsers,
    statusMessage: state.statusMessage,
    updatingUsername: currentUpdatingUsername,
    navigationIntent: state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent,
    savePermissions,
  }
}
