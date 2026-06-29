import type { Username } from '@/objects/user/Username'
import type { UserGroupMember } from '@/objects/usergroup/UserGroupMember'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'

/**
 * 当前查看者对用户组的权限集合，供详情页控制编辑、成员角色和删除入口。
 */
export type UserGroupViewerPermissions = {
  canManage: boolean
  canManageMemberRoles: boolean
  canDelete: boolean
}

/**
 * 根据当前成员关系和站点管理员身份计算用户组详情页权限。
 */
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

/**
 * 判断当前查看者是否可以移除目标成员；站点管理员和 owner/manager 的边界不同。
 */
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
