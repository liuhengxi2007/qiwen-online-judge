import type { DisplayName, Username } from '@/features/auth/model/AuthValues'
import type { UserPreferences } from '@/features/auth/model/UserPreferences'
import type { UserGroupRole } from '@/features/usergroup/model/UserGroupRole'

export type UserGroupMember = {
  username: Username
  displayName: DisplayName
  preferences: UserPreferences
  role: UserGroupRole
  joinedAt: string
}
