import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'

/** 更新用户组成员角色请求体；目标成员由 API path 指定。 */
export type UpdateUserGroupMemberRoleRequest = {
  role: UserGroupRole
}
