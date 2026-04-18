import type { UserPreferences } from './auth'
import type { PageResponse } from './shared'

export type UserGroupRole = 'owner' | 'manager' | 'member'
export type AddUserGroupMemberRole = 'manager' | 'member'

export type UserGroupMember = {
  username: string
  displayName: string
  preferences: UserPreferences
  role: UserGroupRole
  joinedAt: string
}

export type UserGroupSummary = {
  id: string
  slug: string
  name: string
  description: string
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export type UserGroupDetail = {
  id: string
  slug: string
  name: string
  description: string
  ownerUsername: string
  members: UserGroupMember[]
  createdAt: string
  updatedAt: string
}

export type CreateUserGroupRequest = {
  slug: string
  name: string
  description: string
}

export type UpdateUserGroupRequest = {
  name: string
  description: string
}

export type AddUserGroupMemberRequest = {
  username: string
  role: AddUserGroupMemberRole
}

export type UpdateUserGroupMemberRoleRequest = {
  role: UserGroupRole
}

export type UserGroupListResponse = PageResponse<UserGroupSummary>
