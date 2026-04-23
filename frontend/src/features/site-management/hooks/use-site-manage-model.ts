import { useReducer } from 'react'

import type { AuthUserListItem, UpdateUserPermissionsRequest } from '@/features/user/domain/user'
import {
  initialSiteManageState,
  reduceSiteManageState,
} from '@/features/site-management/domain/site-manage-state'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { useSiteManageQuery } from '@/features/site-management/hooks/use-site-manage-query'
import { useUserDeleteMutation } from '@/features/site-management/hooks/use-user-delete-mutation'
import { useUserPermissionsMutation } from '@/features/site-management/hooks/use-user-permissions-mutation'

export function useSiteManageModel(siteManagerEnabled: boolean) {
  const query = useSiteManageQuery(siteManagerEnabled)
  const [state, dispatch] = useReducer(reduceSiteManageState, initialSiteManageState)
  const mutation = useUserPermissionsMutation()
  const deleteMutation = useUserDeleteMutation()
  const currentUpdatingUsername = state.updatingUsername ?? mutation.updatingUsername
  const currentDeletingUsername = deleteMutation.deletingUsername
  const saveUserPermissions = mutation.savePermissions
  const deleteTargetUser = deleteMutation.deleteTargetUser

  async function savePermissions(listedUser: AuthUserListItem, nextPermissions: UpdateUserPermissionsRequest) {
    if (currentUpdatingUsername) {
      return
    }

    dispatch({ type: 'update_started', username: listedUser.username })

    const result = await saveUserPermissions(listedUser.username, nextPermissions)

    switch (result.kind) {
      case 'updated':
        query.replaceUser(result.user)
        dispatch({ type: 'update_succeeded', user: result.user })
        return
      case 'forbidden':
        dispatch({ type: 'redirect_requested', intent: toSiteManageDeniedRedirect() })
        return
      case 'failed':
        dispatch({ type: 'update_failed', message: result.message })
        return
    }
  }

  async function deleteUser(listedUser: AuthUserListItem) {
    if (currentUpdatingUsername || currentDeletingUsername) {
      return
    }

    const username = listedUser.username
    dispatch({ type: 'delete_started', username })

    const result = await deleteTargetUser(listedUser.username)

    switch (result.kind) {
      case 'deleted':
        query.removeUser(username)
        dispatch({ type: 'delete_succeeded', message: result.message })
        return
      case 'forbidden':
        dispatch({ type: 'redirect_requested', intent: toSiteManageDeniedRedirect() })
        return
      case 'failed':
        dispatch({ type: 'update_failed', message: result.message })
    }
  }

  return {
    users: query.users,
    judgers: query.judgers,
    userListError: state.actionErrorMessage || query.userListError,
    judgerListError: query.judgerListError,
    isLoadingUsers: query.isLoadingUsers,
    isLoadingJudgers: query.isLoadingJudgers,
    statusMessage: state.statusMessage,
    updatingUsername: currentUpdatingUsername,
    deletingUsername: currentDeletingUsername,
    navigationIntent: state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent ?? deleteMutation.navigationIntent,
    savePermissions,
    deleteUser,
  }
}
