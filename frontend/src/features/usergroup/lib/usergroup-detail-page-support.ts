import type { Username } from '@/features/user/model/Username'
import { canRemoveUserGroupMember, resolveUserGroupViewerPermissions } from '@/features/usergroup/lib/usergroup-permissions'
import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'

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
