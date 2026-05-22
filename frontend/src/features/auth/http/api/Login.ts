import type {
  LoginRequest,
  LoginResponse,
} from '@/features/auth/domain/auth'
import {
  fromLoginResponseContract,
  toLoginRequestContract,
} from '@/features/auth/domain/auth'
import { postJson } from '@/shared/api/http-client'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson('/api/auth/login', fromLoginResponseContract, toLoginRequestContract(request))
}
