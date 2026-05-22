import type { Username } from '@/features/auth/model/AuthValues'
import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'

export type AddUserGroupMemberRequest = {
  username: Username
  role: AddUserGroupMemberRole
}
