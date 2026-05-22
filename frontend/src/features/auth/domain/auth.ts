import type { ErrorResponse } from '@/shared/model/ErrorResponse'

export type { ErrorResponse }

export {
  emailAddressValue,
  parseEmailAddress,
  parsePlaintextPassword,
  plaintextPasswordValue,
} from '@/features/auth/domain/auth-parsers'
export {
  asProblemManagerSession,
  asSiteManagerSession,
  isProblemManagerSession,
  isSiteManagerSession,
  toAuthSession,
} from '@/features/auth/domain/auth-session'
export type { EmailAddress } from '@/features/auth/model/EmailAddress'
export type { PlaintextPassword } from '@/features/auth/model/PlaintextPassword'
export type { LoginRequest } from '@/features/auth/http/request/LoginRequest'
export type { LoginResponse } from '@/features/auth/http/response/LoginResponse'
export type { RegisterRequest } from '@/features/auth/http/request/RegisterRequest'
export type { RegisterResponse } from '@/features/auth/http/response/RegisterResponse'
export type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
