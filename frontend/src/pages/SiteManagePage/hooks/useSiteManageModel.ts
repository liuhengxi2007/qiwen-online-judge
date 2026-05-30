import { useReducer } from 'react'

import type { AuthUserListItem } from '@/objects/user/response/AuthUserListItem'
import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { UserListRequest } from '@/objects/user/request/UserListRequest'
import {
  initialSiteManageState,
  reduceSiteManageState,
} from '../functions/SiteManageState'
import { toSiteManageDeniedRedirect } from '@/pages/routing/RoutePolicy'
import { useSiteManageQuery } from './useSiteManageQuery'
import { useUserDeleteMutation } from './useUserDeleteMutation'
import { useUserPermissionsMutation } from './useUserPermissionsMutation'

export function useSiteManageModel(siteManagerEnabled: boolean, userListRequest: UserListRequest) {
  const query = useSiteManageQuery(siteManagerEnabled, userListRequest)
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
    userPage: query.userPage,
    userPageSize: query.userPageSize,
    totalUsers: query.totalUsers,
    judgers: query.judgers,
    userListError: state.actionErrorMessage || query.userListError,
    judgerListError: query.judgerListError,
    isLoadingUsers: query.isLoadingUsers,
    isLoadingJudgers: query.isLoadingJudgers,
    notice: state.notice,
    updatingUsername: currentUpdatingUsername,
    deletingUsername: currentDeletingUsername,
    navigationIntent: state.navigationIntent ?? query.navigationIntent ?? mutation.navigationIntent ?? deleteMutation.navigationIntent,
    savePermissions,
    deleteUser,
  }
}
