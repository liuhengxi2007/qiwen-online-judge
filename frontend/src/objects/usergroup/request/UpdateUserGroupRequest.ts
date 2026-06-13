import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'

/** 更新用户组请求体；允许修改名称和描述。 */
export type UpdateUserGroupRequest = {
  name: UserGroupName
  description: UserGroupDescription
}
