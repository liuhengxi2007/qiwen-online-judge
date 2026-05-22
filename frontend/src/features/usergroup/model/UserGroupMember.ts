import type { DisplayName, Username } from '@/features/user/model/UserValues'
import type { UserGroupRole } from '@/features/usergroup/model/UserGroupRole'

export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  role: UserGroupRole
  joinedAt: string
}
