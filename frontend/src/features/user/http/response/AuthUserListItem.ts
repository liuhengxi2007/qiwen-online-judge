import type { DisplayName, Username } from '@/features/user/model/UserValues'
import type { EmailAddress } from '@/features/auth/model/AuthValues'

export type AuthUserListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}
