import type { DisplayName, EmailAddress, Username } from '@/features/auth/model/AuthValues'

export type SessionResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
}
