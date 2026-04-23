import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  SessionResponse,
} from '@/features/auth/domain/auth'
import type { RegisteredJudgerListItem } from '@/features/judger/model/RegisteredJudgerListItem'
import {
  fromLoginResponseContract,
  fromRegisterResponseContract,
  fromSessionResponseContract,
  toLoginRequestContract,
  toRegisterRequestContract,
} from '@/features/auth/domain/auth'
import { fromRegisteredJudgerListItemContract } from '@/features/judger/domain/judger'
import { postJson, requestJson } from '@/shared/api/http-client'

export { HttpClientError as AuthClientError } from '@/shared/api/http-client'

export async function getSession(): Promise<SessionResponse> {
  return requestJson('/api/auth/session', fromSessionResponseContract)
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
  }).catch(() => undefined)
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson('/api/auth/login', fromLoginResponseContract, toLoginRequestContract(request))
}

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson('/api/auth/register', fromRegisterResponseContract, toRegisterRequestContract(request))
}

export async function listRegisteredJudgers(): Promise<RegisteredJudgerListItem[]> {
  return requestJson('/api/auth/judgers', (value) => {
    if (!Array.isArray(value)) {
      throw new Error('Invalid registered judger list payload.')
    }

    return value.map(fromRegisteredJudgerListItemContract)
  })
}
