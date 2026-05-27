import type { DisplayName } from '@/objects/user/DisplayName'
import { fromDisplayNameContract } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import { fromUsernameContract } from '@/objects/user/Username'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'
import { fromUserGroupRoleContract } from '@/objects/usergroup/UserGroupRole'

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

export function fromUserGroupMemberContract(member: UserGroupMemberContract): UserGroupMember {
  return {
    username: fromUsernameContract(member.username, 'user group member username'),
    displayName: fromDisplayNameContract(member.displayName, 'user group member display name'),
    role: fromUserGroupRoleContract(member.role, 'user group member role'),
    joinedAt: member.joinedAt,
  }
}
