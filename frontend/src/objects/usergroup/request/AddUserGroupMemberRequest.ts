import type { Username } from '@/objects/user/Username'
import type { AddUserGroupMemberRole } from '@/objects/usergroup/AddUserGroupMemberRole'

export type AddUserGroupMemberRequest = {
  username: Username
  role: AddUserGroupMemberRole
}
