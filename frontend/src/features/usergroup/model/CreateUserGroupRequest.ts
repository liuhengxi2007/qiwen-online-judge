import type { UserGroupDescription } from '@/features/usergroup/model/UserGroupDescription'
import type { UserGroupName } from '@/features/usergroup/model/UserGroupName'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'

export type CreateUserGroupRequest = {
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
}
