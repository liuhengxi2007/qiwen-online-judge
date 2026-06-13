import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'

/** 创建用户组请求体；owner 由当前会话在后端确定。 */
export type CreateUserGroupRequest = {
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
}
