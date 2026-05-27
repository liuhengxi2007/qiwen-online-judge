import type { Username } from '@/objects/user/Username'
import type { UserGroupMember } from '@/objects/usergroup/UserGroupMember'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'

export type UserGroupViewerPermissions = {
  canManage: boolean
  canManageMemberRoles: boolean
  canDelete: boolean
}

export function resolveUserGroupViewerPermissions(
  members: UserGroupMember[],
  viewerUsername: Username,
  isSiteManager: boolean,
): UserGroupViewerPermissions {
  const currentMembership = members.find((member) => member.username === viewerUsername)
  const role = currentMembership?.role ?? null

  return {
    canManage: isSiteManager || role === 'owner' || role === 'manager',
    canManageMemberRoles: isSiteManager || role === 'owner',
    canDelete: isSiteManager || role === 'owner',
  }
}

export function canRemoveUserGroupMember(
  members: UserGroupMember[],
  viewerUsername: Username,
  isSiteManager: boolean,
  targetRole: UserGroupRole,
): boolean {
  if (isSiteManager) {
    return targetRole !== 'owner'
  }

  const currentMembership = members.find((member) => member.username === viewerUsername)
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
