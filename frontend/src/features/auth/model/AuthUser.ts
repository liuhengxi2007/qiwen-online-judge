import type { DisplayName, EmailAddress, PasswordHash, Username } from '@/features/auth/model/AuthValues'

export type AuthUser = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  passwordHash: PasswordHash
  siteManager: boolean
  problemManager: boolean
}
