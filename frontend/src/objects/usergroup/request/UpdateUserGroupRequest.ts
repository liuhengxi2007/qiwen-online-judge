import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'

export type UpdateUserGroupRequest = {
  name: UserGroupName
  description: UserGroupDescription
}
