import type { DisplayName, Username } from '@/features/auth/model/AuthValues'
import type { UserGroupRole } from '@/features/usergroup/model/UserGroupRole'

export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  role: UserGroupRole
  joinedAt: string
}
