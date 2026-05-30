import { useMemo, useReducer } from 'react'

import type { Username } from '@/objects/user/Username'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import {
  initialUserGroupDetailPageMessageState,
  reduceUserGroupDetailPageMessageState,
} from '../functions/UserGroupDetailPageState'
import {
  canViewerRemoveUserGroupMember,
  resolveUserGroupDetailPermissions,
} from '../functions/UserGroupDetailPageSupport'
import { useUserGroupAddMemberAction } from './useUserGroupAddMemberAction'
import { useUserGroupDeleteAction } from './useUserGroupDeleteAction'
import { useUserGroupDetailQuery } from './useUserGroupDetailQuery'
import { useUserGroupEditorState } from './useUserGroupEditorState'
import { useUserGroupRemoveMemberAction } from './useUserGroupRemoveMemberAction'
import { useUserGroupUpdateMemberRoleAction } from './useUserGroupUpdateMemberRoleAction'
import { useUserGroupUpdateAction } from './useUserGroupUpdateAction'
import { useI18n } from '@/system/i18n/use-i18n'

export function useUserGroupDetailPageModel(userGroupSlug: UserGroupSlug, viewerUsername: Username, isSiteManager: boolean) {
  const { t } = useI18n()
  const detailQuery = useUserGroupDetailQuery(userGroupSlug)
  const editor = useUserGroupEditorState(detailQuery.userGroup)
  const updateAction = useUserGroupUpdateAction(userGroupSlug)
  const deleteAction = useUserGroupDeleteAction(userGroupSlug)
  const addMemberAction = useUserGroupAddMemberAction(userGroupSlug)
  const updateMemberRoleAction = useUserGroupUpdateMemberRoleAction(userGroupSlug)
  const removeMemberAction = useUserGroupRemoveMemberAction(userGroupSlug)
  const [messageState, dispatch] = useReducer(
    reduceUserGroupDetailPageMessageState,
    initialUserGroupDetailPageMessageState,
  )

  const permissions = useMemo(() => {
    return resolveUserGroupDetailPermissions(detailQuery.userGroup, viewerUsername, isSiteManager)
  }, [detailQuery.userGroup, isSiteManager, viewerUsername])

  function canRemoveMember(_targetUsername: Username, targetRole: 'owner' | 'manager' | 'member') {
    return canViewerRemoveUserGroupMember(detailQuery.userGroup, viewerUsername, isSiteManager, targetRole)
  }

  async function save() {
    if (!permissions.canManage) {
      dispatch({ type: 'save_forbidden', message: t('userGroup.message.managePermissionRequired') })
      return
    }

    const result = await updateAction.save({
      name: editor.name,
      description: editor.description,
    })

    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      dispatch({ type: 'save_succeeded', message: result.message })
    } else {
      dispatch({ type: 'save_failed', message: result.message })
    }
  }

  async function addMember() {
    if (!permissions.canManage) {
      dispatch({ type: 'add_member_forbidden', message: t('userGroup.message.managePermissionRequired') })
      return
    }

    const result = await addMemberAction.addMember(editor.memberUsername, editor.memberRole)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      editor.clearMemberDraft()
      dispatch({ type: 'add_member_succeeded', message: result.message })
    } else {
      dispatch({ type: 'add_member_failed', message: result.message })
    }
  }

  async function deleteCurrentUserGroup() {
    if (!permissions.canDelete) {
      dispatch({ type: 'general_forbidden', message: t('userGroup.message.ownerPermissionRequired') })
      return false
    }

    const result = await deleteAction.deleteCurrentUserGroup()
    if (result.ok) {
      dispatch({ type: 'general_succeeded', message: result.message })
      return true
    }

    dispatch({ type: 'general_failed', message: result.message })
    return false
  }

  async function updateMemberRole(targetUsername: Username, role: 'owner' | 'manager' | 'member') {
    if (!permissions.canManageMemberRoles) {
      dispatch({ type: 'general_forbidden', message: t('userGroup.message.ownerPermissionRequired') })
      return false
    }

    const result = await updateMemberRoleAction.updateRole(targetUsername, role)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      dispatch({ type: 'general_succeeded', message: result.message })
      return true
    }

    dispatch({ type: 'general_failed', message: result.message })
    return false
  }

  async function removeMember(targetUsername: Username) {
    if (!permissions.canManage) {
      dispatch({ type: 'general_forbidden', message: t('userGroup.message.managePermissionRequired') })
      return false
    }

    const result = await removeMemberAction.removeMember(targetUsername)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      dispatch({ type: 'general_succeeded', message: result.message })
      return true
    }

    dispatch({ type: 'general_failed', message: result.message })
    return false
  }

  return {
    userGroup: detailQuery.userGroup,
    isLoading: detailQuery.isLoading,
    isSaving: updateAction.isSaving,
    isDeleting: deleteAction.isDeleting,
    isAddingMember: addMemberAction.isAddingMember,
    activeUpdatingUsername: updateMemberRoleAction.activeUpdatingUsername,
    activeRemovingUsername: removeMemberAction.activeRemovingUsername,
    canManage: permissions.canManage,
    canManageMemberRoles: permissions.canManageMemberRoles,
    canDelete: permissions.canDelete,
    name: editor.name,
    description: editor.description,
    memberUsername: editor.memberUsername,
    memberRole: editor.memberRole,
    errorMessage: detailQuery.errorMessage || messageState.generalErrorMessage,
    successMessage: detailQuery.errorMessage ? '' : messageState.generalSuccessMessage,
    saveErrorMessage: messageState.saveErrorMessage,
    saveSuccessMessage: messageState.saveSuccessMessage,
    addMemberErrorMessage: messageState.addMemberErrorMessage,
    addMemberSuccessMessage: messageState.addMemberSuccessMessage,
    setName: editor.setName,
    setDescription: editor.setDescription,
    setMemberUsername: editor.setMemberUsername,
    setMemberRole: editor.setMemberRole,
    save,
    addMember,
    updateMemberRole,
    removeMember,
    deleteCurrentUserGroup,
    canRemoveMember,
  }
}
