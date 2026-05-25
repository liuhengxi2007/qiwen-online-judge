import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'
import type { UserGroupDescription } from '@/features/usergroup/model/UserGroupDescription'
import type { UserGroupId } from '@/features/usergroup/model/UserGroupId'
import type { UserGroupMember } from '@/features/usergroup/model/UserGroupMember'
import type { UserGroupName } from '@/features/usergroup/model/UserGroupName'
import type { UserGroupRole } from '@/features/usergroup/model/UserGroupRole'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import {
  fromDisplayNameContract,
  fromUsernameContract,
  type DisplayNameContract,
  type UsernameContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'
import {
  parseAddUserGroupMemberRole,
  parseUserGroupDescription,
  parseUserGroupId,
  parseUserGroupName,
  parseUserGroupRole,
  parseUserGroupSlug,
  type ParseResult,
  userGroupDescriptionValue,
  userGroupNameValue,
  userGroupSlugValue,
} from '@/features/usergroup/lib/usergroup-parsers'

export type UserGroupIdContract = string
export type UserGroupSlugContract = string
export type UserGroupNameContract = string
export type UserGroupDescriptionContract = string
export type UserGroupRoleContract = 'owner' | 'manager' | 'member'
export type AddUserGroupMemberRoleContract = 'manager' | 'member'

export type UserGroupMemberContract = {
  username: UsernameContract
  displayName: DisplayNameContract
  role: UserGroupRoleContract
  joinedAt: string
}

function requireParsed<T>(result: ParseResult<T>, label: string): T {
  if (!result.ok) {
    throw new Error(`Invalid ${label} in contract payload: ${result.error}`)
  }

  return result.value
}

export function fromUserGroupIdContract(value: UserGroupIdContract, label: string): UserGroupId {
  return requireParsed(parseUserGroupId(value), label)
}

export function fromUserGroupSlugContract(value: UserGroupSlugContract, label: string): UserGroupSlug {
  return requireParsed(parseUserGroupSlug(value), label)
}

export function toUserGroupSlugContract(value: UserGroupSlug): UserGroupSlugContract {
  return userGroupSlugValue(value)
}

export function fromUserGroupNameContract(value: UserGroupNameContract, label: string): UserGroupName {
  return requireParsed(parseUserGroupName(value), label)
}

export function toUserGroupNameContract(value: UserGroupName): UserGroupNameContract {
  return userGroupNameValue(value)
}

export function fromUserGroupDescriptionContract(
  value: UserGroupDescriptionContract,
  label: string,
): UserGroupDescription {
  return requireParsed(parseUserGroupDescription(value), label)
}

export function toUserGroupDescriptionContract(
  value: UserGroupDescription,
): UserGroupDescriptionContract {
  return userGroupDescriptionValue(value)
}

export function fromUserGroupRoleContract(value: UserGroupRoleContract, label: string): UserGroupRole {
  return requireParsed(parseUserGroupRole(value), label)
}

export function toUserGroupRoleContract(value: UserGroupRole): UserGroupRoleContract {
  return value
}

export function fromAddUserGroupMemberRoleContract(
  value: AddUserGroupMemberRoleContract,
  label: string,
): AddUserGroupMemberRole {
  return requireParsed(parseAddUserGroupMemberRole(value), label)
}

export function toAddUserGroupMemberRoleContract(
  value: AddUserGroupMemberRole,
): AddUserGroupMemberRoleContract {
  return value
}

export function fromUserGroupMemberContract(member: UserGroupMemberContract): UserGroupMember {
  return {
    username: fromUsernameContract(member.username, 'user group member username'),
    displayName: fromDisplayNameContract(member.displayName, 'user group member display name'),
    role: fromUserGroupRoleContract(member.role, 'user group member role'),
    joinedAt: member.joinedAt,
  }
}
