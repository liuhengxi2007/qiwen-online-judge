import type { Username } from '@/objects/user/Username'
import { canRemoveUserGroupMember, resolveUserGroupViewerPermissions } from './UserGroupPermissions'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'

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
