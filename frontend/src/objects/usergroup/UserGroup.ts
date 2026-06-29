import type { Username } from '@/objects/user/Username'
import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupId } from '@/objects/usergroup/UserGroupId'
import type { UserGroupMember } from '@/objects/usergroup/UserGroupMember'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { AuditFields } from '@/objects/shared/AuditFields'

/** 用户组完整对象；包含 owner、成员列表和审计字段。 */
export type UserGroup = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
  members: UserGroupMember[]
}
