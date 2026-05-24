import type { AccessUserGroupSlug } from '@/shared/model/access/AccessUserGroupSlug'
import type { AccessUsername } from '@/shared/model/access/AccessUsername'

export type UserAccessSubject = {
  kind: 'user'
  username: AccessUsername
}

export type UserGroupAccessSubject = {
  kind: 'user_group'
  slug: AccessUserGroupSlug
}

export type AccessSubject = UserAccessSubject | UserGroupAccessSubject
