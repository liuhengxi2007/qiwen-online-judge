import type { LoginRequest } from '@/features/auth/model/request/LoginRequest'
import type { LoginResponse } from '@/features/auth/model/response/LoginResponse'
import type { RegisterRequest } from '@/features/auth/model/request/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/model/response/RegisterResponse'
import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import {
  fromEmailAddressContract,
  toEmailAddressContract,
  toPlaintextPasswordContract,
} from '@/features/auth/http/codec/AuthModelHttpCodecs'
import type {
  UserPreferencesContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'
import {
  fromDisplayNameContract,
  fromUserPreferencesContract,
  fromUsernameContract,
  toDisplayNameContract,
  toUsernameContract,
} from '@/features/user/http/codec/UserModelHttpCodecs'

type LoginRequestContract = {
  username: string
  password: string
}

type LoginResponseContract = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferencesContract
  siteManager: boolean
  problemManager: boolean
  message: string
}

type RegisterRequestContract = {
  username: string
  displayName: string
  email: string
  password: string
}

type RegisterResponseContract = LoginResponseContract

type SessionResponseContract = {
  displayName: string
  username: string
  email: string
  preferences: UserPreferencesContract
  siteManager: boolean
  problemManager: boolean
}

export function toLoginRequestContract(request: LoginRequest): LoginRequestContract {
  return {
    username: toUsernameContract(request.username),
    password: toPlaintextPasswordContract(request.password),
  }
}

export function fromLoginResponseContract(response: LoginResponseContract): LoginResponse {
  return {
    displayName: fromDisplayNameContract(response.displayName, 'login response display name'),
    username: fromUsernameContract(response.username, 'login response username'),
    email: fromEmailAddressContract(response.email, 'login response email'),
    preferences: fromUserPreferencesContract(response.preferences, 'login response'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
    message: response.message,
  }
}

export function toRegisterRequestContract(request: RegisterRequest): RegisterRequestContract {
  return {
    username: toUsernameContract(request.username),
    displayName: toDisplayNameContract(request.displayName),
    email: toEmailAddressContract(request.email),
    password: toPlaintextPasswordContract(request.password),
  }
}

export function fromRegisterResponseContract(response: RegisterResponseContract): RegisterResponse {
  return fromLoginResponseContract(response)
}

export function fromSessionResponseContract(response: SessionResponseContract): SessionResponse {
  return {
    displayName: fromDisplayNameContract(response.displayName, 'session response display name'),
    username: fromUsernameContract(response.username, 'session response username'),
    email: fromEmailAddressContract(response.email, 'session response email'),
    preferences: fromUserPreferencesContract(response.preferences, 'session response'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}
