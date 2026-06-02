import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { UserAvatarUrl } from '@/objects/user/UserAvatarUrl'
import type { UserPreferences } from '@/objects/user/UserPreferences'

export type SessionResponse = {
  displayName: DisplayName
  username: Username
  avatarUrl: UserAvatarUrl | null
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
  contestManager: boolean
}
