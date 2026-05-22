import type { Username } from '@/features/user/model/UserValues'
import type { UserGroupDescription } from '@/features/usergroup/model/UserGroupDescription'
import type { UserGroupId } from '@/features/usergroup/model/UserGroupId'
import type { UserGroupName } from '@/features/usergroup/model/UserGroupName'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import type { AuditFields } from '@/shared/model/AuditFields'

export type UserGroupSummary = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
}
