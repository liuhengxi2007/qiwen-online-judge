import type { Username } from '@/objects/user/Username'
import type { NewUserGroupMemberRole } from '@/objects/usergroup/NewUserGroupMemberRole'

export type AddUserGroupMemberRequest = {
  username: Username
  role: NewUserGroupMemberRole
}
