import { useMemo, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import type { UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { useUserGroupAddMemberAction } from '@/features/usergroup/hooks/use-usergroup-add-member-action'
import { useUserGroupDeleteAction } from '@/features/usergroup/hooks/use-usergroup-delete-action'
import { useUserGroupDetailQuery } from '@/features/usergroup/hooks/use-usergroup-detail-query'
import { useUserGroupEditorState } from '@/features/usergroup/hooks/use-usergroup-editor-state'
import { useUserGroupRemoveMemberAction } from '@/features/usergroup/hooks/use-usergroup-remove-member-action'
import { useUserGroupUpdateMemberRoleAction } from '@/features/usergroup/hooks/use-usergroup-update-member-role-action'
import { useUserGroupUpdateAction } from '@/features/usergroup/hooks/use-usergroup-update-action'

export function useUserGroupDetailPageModel(userGroupSlug: UserGroupSlug, viewerUsername: Username, isSiteManager: boolean) {
  const detailQuery = useUserGroupDetailQuery(userGroupSlug)
  const editor = useUserGroupEditorState(detailQuery.userGroup)
  const updateAction = useUserGroupUpdateAction(userGroupSlug)
  const deleteAction = useUserGroupDeleteAction(userGroupSlug)
  const addMemberAction = useUserGroupAddMemberAction(userGroupSlug)
  const updateMemberRoleAction = useUserGroupUpdateMemberRoleAction(userGroupSlug)
  const removeMemberAction = useUserGroupRemoveMemberAction(userGroupSlug)
  const [messageState, setMessageState] = useState<{ errorMessage: string; successMessage: string }>({
    errorMessage: '',
    successMessage: '',
  })

  const permissions = useMemo(() => {
    const memberships = detailQuery.userGroup?.members ?? []
    const currentMembership = memberships.find((member) => member.username === viewerUsername)
    const role = currentMembership?.role ?? null
    const canManage = isSiteManager || role === 'owner' || role === 'manager'
    const canManageMemberRoles = isSiteManager || role === 'owner'
    const canDelete = isSiteManager || role === 'owner'

    return { canManage, canManageMemberRoles, canDelete }
  }, [detailQuery.userGroup?.members, isSiteManager, viewerUsername])

  async function save() {
    if (!permissions.canManage) {
      setMessageState({ errorMessage: 'Owner, manager, or site manager permission required.', successMessage: '' })
      return
    }

    const result = await updateAction.save({
      name: editor.name,
      description: editor.description,
    })

    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      setMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function addMember() {
    if (!permissions.canManage) {
      setMessageState({ errorMessage: 'Owner, manager, or site manager permission required.', successMessage: '' })
      return
    }

    const result = await addMemberAction.addMember(editor.memberUsername, editor.memberRole)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      editor.clearMemberDraft()
      setMessageState({ errorMessage: '', successMessage: result.message })
    } else {
      setMessageState({ errorMessage: result.message, successMessage: '' })
    }
  }

  async function deleteCurrentUserGroup() {
    if (!permissions.canDelete) {
      setMessageState({ errorMessage: 'Owner or site manager permission required.', successMessage: '' })
      return false
    }

    const result = await deleteAction.deleteCurrentUserGroup()
    if (result.ok) {
      setMessageState({ errorMessage: '', successMessage: result.message })
      return true
    }

    setMessageState({ errorMessage: result.message, successMessage: '' })
    return false
  }

  async function updateMemberRole(targetUsername: Username, role: 'owner' | 'manager' | 'member') {
    if (!permissions.canManageMemberRoles) {
      setMessageState({ errorMessage: 'Owner or site manager permission required.', successMessage: '' })
      return false
    }

    const result = await updateMemberRoleAction.updateRole(targetUsername, role)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      setMessageState({ errorMessage: '', successMessage: result.message })
      return true
    }

    setMessageState({ errorMessage: result.message, successMessage: '' })
    return false
  }

  async function removeMember(targetUsername: Username) {
    if (!permissions.canManage) {
      setMessageState({ errorMessage: 'Owner, manager, or site manager permission required.', successMessage: '' })
      return false
    }

    const result = await removeMemberAction.removeMember(targetUsername)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      setMessageState({ errorMessage: '', successMessage: result.message })
      return true
    }

    setMessageState({ errorMessage: result.message, successMessage: '' })
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
    errorMessage: detailQuery.errorMessage || messageState.errorMessage,
    successMessage: detailQuery.errorMessage ? '' : messageState.successMessage,
    setName: editor.setName,
    setDescription: editor.setDescription,
    setMemberUsername: editor.setMemberUsername,
    setMemberRole: editor.setMemberRole,
    save,
    addMember,
    updateMemberRole,
    removeMember,
    deleteCurrentUserGroup,
  }
}
