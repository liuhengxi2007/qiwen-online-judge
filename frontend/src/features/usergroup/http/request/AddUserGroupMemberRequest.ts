import type { Username } from '@/features/user/model/UserValues'
import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'

export type AddUserGroupMemberRequest = {
  username: Username
  role: AddUserGroupMemberRole
}
