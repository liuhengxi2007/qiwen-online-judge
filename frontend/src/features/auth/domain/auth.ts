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
export {
  fromLoginResponseContract,
  fromRegisterResponseContract,
  fromSessionResponseContract,
  toLoginRequestContract,
  toRegisterRequestContract,
} from '@/features/auth/domain/auth-contract'

export type { EmailAddress, PlaintextPassword } from '@/features/auth/model/AuthValues'
export type { LoginRequest } from '@/features/auth/http/request/LoginRequest'
export type { LoginResponse } from '@/features/auth/http/response/LoginResponse'
export type { RegisterRequest } from '@/features/auth/http/request/RegisterRequest'
export type { RegisterResponse } from '@/features/auth/http/response/RegisterResponse'
export type { SessionResponse } from '@/features/auth/http/response/SessionResponse'
