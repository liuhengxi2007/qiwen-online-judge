import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'
import { fromUserGroupRoleContract } from '@/objects/usergroup/UserGroupRole'
import { readRecord, readString } from '@/objects/shared/PageResponse'

export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  role: UserGroupRole
  joinedAt: string
}

type UserGroupMemberContract = {
  username: string
  displayName: string
  role: UserGroupRole
  joinedAt: string
}

export function fromUserGroupMemberContract(value: unknown, label = 'user group member'): UserGroupMember {
  const member = readRecord(value, label) as UserGroupMemberContract
  return {
    username: fromUsernameContract(readString(member.username, `${label} username`), `${label} username`),
    displayName: fromDisplayNameContract(readString(member.displayName, `${label} display name`), `${label} display name`),
    role: fromUserGroupRoleContract(readString(member.role, `${label} role`), `${label} role`),
    joinedAt: readString(member.joinedAt, `${label} joined at`),
  }
}
