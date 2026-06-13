import type { Username } from '@/objects/user/Username'
import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupId } from '@/objects/usergroup/UserGroupId'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { AuditFields } from '@/objects/shared/AuditFields'

/** 用户组摘要响应；用于列表展示，不包含成员列表。 */
export type UserGroupSummary = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
}
