import type { Username } from '@/objects/user/Username'
import type { NewUserGroupMemberRole } from '@/objects/usergroup/request/NewUserGroupMemberRole'

/** 添加用户组成员请求体；目标用户组由 API path 指定。 */
export type AddUserGroupMemberRequest = {
  username: Username
  role: NewUserGroupMemberRole
}
