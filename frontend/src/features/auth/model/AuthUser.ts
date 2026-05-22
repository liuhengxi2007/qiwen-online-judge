import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'
import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { PasswordHash } from '@/features/auth/model/PasswordHash'

export type AuthUser = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  passwordHash: PasswordHash
  siteManager: boolean
  problemManager: boolean
}
