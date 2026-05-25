import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { DisplayName } from '@/features/user/model/DisplayName'
import type { Username } from '@/features/user/model/Username'

export type AuthAccountListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}
