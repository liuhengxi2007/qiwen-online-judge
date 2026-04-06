import { useCallback, useReducer } from 'react'

import { usernameValue, type AuthUserListItem, type UpdateUserPermissionsRequest, type Username } from '@/features/auth/domain/auth'
import { useSiteManageQuery } from '@/features/site-management/hooks/use-site-manage-query'
import { useUserDeleteMutation } from '@/features/site-management/hooks/use-user-delete-mutation'
import { useUserPermissionsMutation } from '@/features/site-management/hooks/use-user-permissions-mutation'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'

type SiteManageState = {
  statusMessage: string
  actionErrorMessage: string
  updatingUsername: Username | null
  navigationIntent: NavigationIntent | null
}

type SiteManageAction =
  | { type: 'update_started'; username: Username }
  | { type: 'update_succeeded'; user: AuthUserListItem }
  | { type: 'delete_started'; username: Username }
  | { type: 'delete_succeeded'; username: Username; message: string }
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
    case 'delete_started':
      return {
        ...state,
        updatingUsername: action.username,
        statusMessage: '',
        actionErrorMessage: '',
      }
    case 'delete_succeeded':
      return {
        ...state,
        updatingUsername: null,
        statusMessage: action.message,
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
  const deleteMutation = useUserDeleteMutation()
  const currentUpdatingUsername = state.updatingUsername ?? mutation.updatingUsername
  const currentDeletingUsername = deleteMutation.deletingUsername
  const saveUserPermissions = mutation.savePermissions
  const deleteTargetUser = deleteMutation.deleteTargetUser

  const savePermissions = useCallback(
    async (listedUser: AuthUserListItem, nextPermissions: UpdateUserPermissionsRequest) => {
      if (currentUpdatingUsername) {
        return
      }

      dispatch({ type: 'update_started', username: listedUser.username })

      const result = await saveUserPermissions(listedUser.username, nextPermissions)

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
    [currentUpdatingUsername, saveUserPermissions],
  )

  const deleteUser = useCallback(
    async (listedUser: AuthUserListItem) => {
      if (currentUpdatingUsername || currentDeletingUsername) {
        return
      }

      const username = listedUser.username
      dispatch({ type: 'delete_started', username })

      const result = await deleteTargetUser(listedUser.username)

      switch (result.kind) {
        case 'deleted':
          dispatch({ type: 'delete_succeeded', username, message: result.message })
          return
        case 'forbidden':
          dispatch({ type: 'redirect_requested', intent: { to: '/?notice=site-manage-denied', replace: true } })
          return
        case 'failed':
          dispatch({ type: 'update_failed', message: result.message })
      }
    },
    [currentDeletingUsername, currentUpdatingUsername, deleteTargetUser],
  )

  return {
    users: query.users,
    userListError: state.actionErrorMessage || query.userListError,
    isLoadingUsers: query.isLoadingUsers,
    statusMessage: state.statusMessage,
    updatingUsername: currentUpdatingUsername,
    deletingUsername: currentDeletingUsername,
    navigationIntent: state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent ?? deleteMutation.navigationIntent,
    savePermissions,
    deleteUser,
  }
}
