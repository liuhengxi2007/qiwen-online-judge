import type { DisplayName } from '@/objects/user/DisplayName'
import { fromSessionResponseContract } from '@/objects/auth/response/SessionResponse'
import type { Username } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'
import { readRecord, readString } from '@/objects/shared/PageResponse'
import type { UserPreferences } from '@/objects/user/UserPreferences'

export type RegisterResponse = {
  displayName: DisplayName
  username: Username
  email: EmailAddress
  preferences: UserPreferences
  siteManager: boolean
  problemManager: boolean
  message: string
}

export function fromRegisterResponseContract(value: unknown, label = 'register response'): RegisterResponse {
  const response = readRecord(value, label)
  return {
    ...fromSessionResponseContract(value, label),
    message: readString(response.message, `${label} message`),
  }
}
