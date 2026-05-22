import type { Username } from '@/features/user/domain/user'
import { canRemoveUserGroupMember, resolveUserGroupViewerPermissions } from '@/features/usergroup/domain/usergroup-permissions'
import type { UserGroupDetail } from '@/features/usergroup/domain/usergroup'

export function resolveUserGroupDetailPermissions(
  userGroup: UserGroupDetail | null,
  viewerUsername: Username,
  isSiteManager: boolean,
) {
  return resolveUserGroupViewerPermissions(userGroup?.members ?? [], viewerUsername, isSiteManager)
}

export function canViewerRemoveUserGroupMember(
  userGroup: UserGroupDetail | null,
  viewerUsername: Username,
  isSiteManager: boolean,
  targetRole: 'owner' | 'manager' | 'member',
) {
  return canRemoveUserGroupMember(userGroup?.members ?? [], viewerUsername, isSiteManager, targetRole)
}
