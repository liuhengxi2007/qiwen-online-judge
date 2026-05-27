import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'

export type AuthAccountListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}
