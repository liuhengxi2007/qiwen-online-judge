import type { UserGroupDescription } from '@/features/usergroup/model/UserGroupDescription'
import type { UserGroupName } from '@/features/usergroup/model/UserGroupName'

export type UpdateUserGroupRequest = {
  name: UserGroupName
  description: UserGroupDescription
}
