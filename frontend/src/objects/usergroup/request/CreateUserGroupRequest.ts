import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'

export type CreateUserGroupRequest = {
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
}
