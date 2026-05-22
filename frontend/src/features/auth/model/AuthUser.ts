import type { DisplayName, Username } from '@/features/user/model/UserValues'
import type { EmailAddress, PasswordHash } from '@/features/auth/model/AuthValues'

export type AuthUser = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  passwordHash: PasswordHash
  siteManager: boolean
  problemManager: boolean
}
