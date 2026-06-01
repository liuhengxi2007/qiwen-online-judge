import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { UserGroupDescription } from '@/objects/usergroup/UserGroupDescription'
import { fromUserGroupDescriptionContract } from '@/objects/usergroup/UserGroupDescription'
import type { UserGroupId } from '@/objects/usergroup/UserGroupId'
import { fromUserGroupIdContract } from '@/objects/usergroup/UserGroupId'
import type { UserGroupName } from '@/objects/usergroup/UserGroupName'
import { fromUserGroupNameContract } from '@/objects/usergroup/UserGroupName'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { fromUserGroupSlugContract } from '@/objects/usergroup/UserGroupSlug'
import type { AuditFields } from '@/objects/shared/AuditFields'
import { fromAuditFieldsContract } from '@/objects/shared/AuditFields'
import { readRecord, readString } from '@/objects/shared/PageResponse'

export type UserGroupSummary = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
}

export function fromUserGroupSummaryContract(value: unknown, label: string): UserGroupSummary {
  const group = readRecord(value, label)
  return {
    ...fromAuditFieldsContract(value, label),
    id: fromUserGroupIdContract(readString(group.id, `${label} id`), `${label} id`),
    slug: fromUserGroupSlugContract(readString(group.slug, `${label} slug`), `${label} slug`),
    name: fromUserGroupNameContract(readString(group.name, `${label} name`), `${label} name`),
    description: fromUserGroupDescriptionContract(readString(group.description, `${label} description`), `${label} description`),
    ownerUsername: fromUsernameContract(readString(group.ownerUsername, `${label} owner username`), `${label} owner username`),
  }
}
