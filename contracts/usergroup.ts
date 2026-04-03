import type { PageResponse } from './shared'

export type UserGroupRole = 'owner' | 'manager' | 'member'

export type UserGroupMember = {
  username: string
  displayName: string
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
  role: UserGroupRole
}

export type UserGroupListResponse = PageResponse<UserGroupSummary>
