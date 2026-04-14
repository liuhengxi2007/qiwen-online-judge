import type { DisplayName, EmailAddress, Username } from '@/features/auth/model/AuthValues'

export type RegisterResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  siteManager: boolean
  problemManager: boolean
  message: string
}
