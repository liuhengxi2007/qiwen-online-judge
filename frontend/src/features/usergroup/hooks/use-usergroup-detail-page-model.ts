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
  const [messageState, setMessageState] = useState<{
    generalErrorMessage: string
    generalSuccessMessage: string
    saveErrorMessage: string
    saveSuccessMessage: string
    addMemberErrorMessage: string
    addMemberSuccessMessage: string
  }>({
    generalErrorMessage: '',
    generalSuccessMessage: '',
    saveErrorMessage: '',
    saveSuccessMessage: '',
    addMemberErrorMessage: '',
    addMemberSuccessMessage: '',
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

  function canRemoveMember(_targetUsername: Username, targetRole: 'owner' | 'manager' | 'member') {
    if (isSiteManager) {
      return targetRole !== 'owner'
    }

    const memberships = detailQuery.userGroup?.members ?? []
    const currentMembership = memberships.find((member) => member.username === viewerUsername)
    if (!currentMembership) {
      return false
    }

    if (currentMembership.role === 'owner') {
      return targetRole !== 'owner'
    }

    if (currentMembership.role === 'manager') {
      return targetRole === 'member'
    }

    return false
  }

  async function save() {
    if (!permissions.canManage) {
      setMessageState((current) => ({
        ...current,
        saveErrorMessage: 'Owner, manager, or site manager permission required.',
        saveSuccessMessage: '',
      }))
      return
    }

    const result = await updateAction.save({
      name: editor.name,
      description: editor.description,
    })

    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      setMessageState((current) => ({
        ...current,
        saveErrorMessage: '',
        saveSuccessMessage: result.message,
      }))
    } else {
      setMessageState((current) => ({
        ...current,
        saveErrorMessage: result.message,
        saveSuccessMessage: '',
      }))
    }
  }

  async function addMember() {
    if (!permissions.canManage) {
      setMessageState((current) => ({
        ...current,
        addMemberErrorMessage: 'Owner, manager, or site manager permission required.',
        addMemberSuccessMessage: '',
      }))
      return
    }

    const result = await addMemberAction.addMember(editor.memberUsername, editor.memberRole)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      editor.clearMemberDraft()
      setMessageState((current) => ({
        ...current,
        addMemberErrorMessage: '',
        addMemberSuccessMessage: result.message,
      }))
    } else {
      setMessageState((current) => ({
        ...current,
        addMemberErrorMessage: result.message,
        addMemberSuccessMessage: '',
      }))
    }
  }

  async function deleteCurrentUserGroup() {
    if (!permissions.canDelete) {
      setMessageState((current) => ({
        ...current,
        generalErrorMessage: 'Owner or site manager permission required.',
        generalSuccessMessage: '',
      }))
      return false
    }

    const result = await deleteAction.deleteCurrentUserGroup()
    if (result.ok) {
      setMessageState((current) => ({
        ...current,
        generalErrorMessage: '',
        generalSuccessMessage: result.message,
      }))
      return true
    }

    setMessageState((current) => ({
      ...current,
      generalErrorMessage: result.message,
      generalSuccessMessage: '',
    }))
    return false
  }

  async function updateMemberRole(targetUsername: Username, role: 'owner' | 'manager' | 'member') {
    if (!permissions.canManageMemberRoles) {
      setMessageState((current) => ({
        ...current,
        generalErrorMessage: 'Owner or site manager permission required.',
        generalSuccessMessage: '',
      }))
      return false
    }

    const result = await updateMemberRoleAction.updateRole(targetUsername, role)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      setMessageState((current) => ({
        ...current,
        generalErrorMessage: '',
        generalSuccessMessage: result.message,
      }))
      return true
    }

    setMessageState((current) => ({
      ...current,
      generalErrorMessage: result.message,
      generalSuccessMessage: '',
    }))
    return false
  }

  async function removeMember(targetUsername: Username) {
    if (!permissions.canManage) {
      setMessageState((current) => ({
        ...current,
        generalErrorMessage: 'Owner, manager, or site manager permission required.',
        generalSuccessMessage: '',
      }))
      return false
    }

    const result = await removeMemberAction.removeMember(targetUsername)
    if (result.ok) {
      detailQuery.replaceUserGroup(result.userGroup)
      setMessageState((current) => ({
        ...current,
        generalErrorMessage: '',
        generalSuccessMessage: result.message,
      }))
      return true
    }

    setMessageState((current) => ({
      ...current,
      generalErrorMessage: result.message,
      generalSuccessMessage: '',
    }))
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
