import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'

export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  role: UserGroupRole
  joinedAt: string
}
