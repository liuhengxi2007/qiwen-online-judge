import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import { fromUserGroupDescriptionContract } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupId } from '@/objects/usergroup/UserGroupId'
import { fromUserGroupIdContract } from '@/objects/usergroup/UserGroupId'
import type { UserGroupMember } from '@/objects/usergroup/UserGroupMember'
import { fromUserGroupMemberContract } from '@/objects/usergroup/UserGroupMember'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import { fromUserGroupNameContract } from '@/objects/usergroup/UserGroupName'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { fromUserGroupSlugContract } from '@/objects/usergroup/UserGroupSlug'
import type { AuditFields } from '@/objects/shared/AuditFields'
import { fromAuditFieldsContract } from '@/objects/shared/AuditFields'
import { readArray, readRecord, readString } from '@/objects/shared/PageResponse'

export type UserGroupDetail = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
  members: UserGroupMember[]
}

export function fromUserGroupDetailContract(value: unknown, label = 'user group detail'): UserGroupDetail {
  const group = readRecord(value, label)
  return {
    ...fromAuditFieldsContract(value, label),
    id: fromUserGroupIdContract(readString(group.id, `${label} id`), `${label} id`),
    slug: fromUserGroupSlugContract(readString(group.slug, `${label} slug`), `${label} slug`),
    name: fromUserGroupNameContract(readString(group.name, `${label} name`), `${label} name`),
    description: fromUserGroupDescriptionContract(readString(group.description, `${label} description`), `${label} description`),
    ownerUsername: fromUsernameContract(readString(group.ownerUsername, `${label} owner username`), `${label} owner username`),
    members: readArray(group.members, `${label} members`, fromUserGroupMemberContract),
  }
}
