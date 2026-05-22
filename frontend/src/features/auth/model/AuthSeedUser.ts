import type { DisplayName, Username } from '@/features/user/model/UserValues'
import type { EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'

export type AuthSeedUser = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  password: PlaintextPassword
  siteManager: boolean
  problemManager: boolean
}
