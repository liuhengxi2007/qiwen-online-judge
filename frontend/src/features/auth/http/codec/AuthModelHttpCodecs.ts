import type { EmailAddress } from '@/features/auth/model/EmailAddress'
import type { PlaintextPassword } from '@/features/auth/model/PlaintextPassword'
import {
  emailAddressValue,
  parseEmailAddress,
  parsePlaintextPassword,
  plaintextPasswordValue,
} from '@/features/auth/lib/auth-parsers'
import { requireParsed } from '@/features/user/lib/user-parsers'

export type EmailAddressContract = string
export type PlaintextPasswordContract = string

export function fromEmailAddressContract(value: EmailAddressContract, label: string): EmailAddress {
  return requireParsed(parseEmailAddress(value), label)
}

export function toEmailAddressContract(value: EmailAddress): EmailAddressContract {
  return emailAddressValue(value)
}

export function fromPlaintextPasswordContract(value: PlaintextPasswordContract, label: string): PlaintextPassword {
  return requireParsed(parsePlaintextPassword(value), label)
}

export function toPlaintextPasswordContract(value: PlaintextPassword): PlaintextPasswordContract {
  return plaintextPasswordValue(value)
}
