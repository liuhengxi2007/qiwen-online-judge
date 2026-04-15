import type { DisplayName, EmailAddress, PlaintextPassword, Username } from '@/features/auth/model/AuthValues'

export type AuthSeedUser = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
  siteManager: boolean
  problemManager: boolean
}
