import type { DisplayName, EmailAddress, Username } from '@/features/auth/model/AuthValues'

export type AuthUserListItem = {
  username: Username
  displayName: DisplayName
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}
