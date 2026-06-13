import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'

/** 用户组成员；包含成员公开身份、角色和加入时间。 */
export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  role: UserGroupRole
  joinedAt: string
}
