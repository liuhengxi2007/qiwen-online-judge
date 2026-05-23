import type { LoginRequest } from '@/features/auth/http/request/LoginRequest'
import type { LoginResponse } from '@/features/auth/http/response/LoginResponse'
import {
  fromLoginResponseContract,
  toLoginRequestContract,
} from '@/features/auth/http/codec'
import { postJson } from '@/shared/api/http-client'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson('/api/auth/login', fromLoginResponseContract, toLoginRequestContract(request))
}
