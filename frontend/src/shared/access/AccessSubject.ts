import type { Username } from '@/features/user/model/Username'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'

export type UserAccessSubject = {
  kind: 'user'
  username: Username
}

export type UserGroupAccessSubject = {
  kind: 'user_group'
  slug: UserGroupSlug
}

export type AccessSubject = UserAccessSubject | UserGroupAccessSubject
