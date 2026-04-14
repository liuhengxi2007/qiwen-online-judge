import type { DisplayName, Username } from '@/features/auth/model/AuthValues'
import type { AuditFields } from '@/shared/model/AuditFields'
import type { PageResponse } from '@/shared/model/Pagination'

export type UserGroupId = string & { readonly __brand: 'UserGroupId' }
export type UserGroupSlug = string & { readonly __brand: 'UserGroupSlug' }
export type UserGroupName = string & { readonly __brand: 'UserGroupName' }
export type UserGroupDescription = string & { readonly __brand: 'UserGroupDescription' }
export type UserGroupRole = 'owner' | 'manager' | 'member'
export type AddUserGroupMemberRole = 'manager' | 'member'

export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  role: UserGroupRole
  joinedAt: string
}

export type UserGroupSummary = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
}

export type UserGroup = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
  members: UserGroupMember[]
}

export type UserGroupDetail = AuditFields & {
  id: UserGroupId
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
  ownerUsername: Username
  members: UserGroupMember[]
}

export type CreateUserGroupRequest = {
  slug: UserGroupSlug
  name: UserGroupName
  description: UserGroupDescription
}

export type UpdateUserGroupRequest = {
  name: UserGroupName
  description: UserGroupDescription
}

export type AddUserGroupMemberRequest = {
  username: Username
  role: AddUserGroupMemberRole
}

export type UpdateUserGroupMemberRoleRequest = {
  role: UserGroupRole
}

export type UserGroupListResponse = PageResponse<UserGroupSummary>

export type ManagedUserGroup = {
  value: UserGroup
}

export type OwnedUserGroup = {
  value: UserGroup
}
