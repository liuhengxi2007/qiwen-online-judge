import type { LoginRequest } from '@/features/auth/model/request/LoginRequest'
import type { LoginResponse } from '@/features/auth/model/response/LoginResponse'
import type { RegisterRequest } from '@/features/auth/model/request/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/model/response/RegisterResponse'
import type { AuthAccountListItem } from '@/features/auth/model/response/AuthAccountListItem'
import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import type { UpdateManagedUserAccountRequest } from '@/features/auth/model/request/UpdateManagedUserAccountRequest'
import type { UpdateOwnAccountRequest } from '@/features/auth/model/request/UpdateOwnAccountRequest'
import type { UpdateUserPermissionsRequest } from '@/features/auth/model/request/UpdateUserPermissionsRequest'
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

type AuthAccountListItemContract = {
  username: string
  displayName: string
  email: string
  siteManager: boolean
  problemManager: boolean
}

type UpdateUserPermissionsRequestContract = {
  siteManager: boolean
  problemManager: boolean
}

type UpdateOwnAccountRequestContract = {
  email: string
  currentPassword: string
  newPassword: string | null
}

type UpdateManagedUserAccountRequestContract = {
  email: string
  newPassword: string | null
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

export function fromAuthAccountListItemContract(response: AuthAccountListItemContract): AuthAccountListItem {
  return {
    username: fromUsernameContract(response.username, 'auth user username'),
    displayName: fromDisplayNameContract(response.displayName, 'auth user display name'),
    email: fromEmailAddressContract(response.email, 'auth user email'),
    siteManager: response.siteManager,
    problemManager: response.problemManager,
  }
}

export function toUpdateUserPermissionsRequestContract(
  request: UpdateUserPermissionsRequest,
): UpdateUserPermissionsRequestContract {
  return request
}

export function toUpdateOwnAccountRequestContract(
  request: UpdateOwnAccountRequest,
): UpdateOwnAccountRequestContract {
  return {
    email: toEmailAddressContract(request.email),
    currentPassword: toPlaintextPasswordContract(request.currentPassword),
    newPassword: request.newPassword ? toPlaintextPasswordContract(request.newPassword) : null,
  }
}

export function toUpdateManagedUserAccountRequestContract(
  request: UpdateManagedUserAccountRequest,
): UpdateManagedUserAccountRequestContract {
  return {
    email: toEmailAddressContract(request.email),
    newPassword: request.newPassword ? toPlaintextPasswordContract(request.newPassword) : null,
  }
}
