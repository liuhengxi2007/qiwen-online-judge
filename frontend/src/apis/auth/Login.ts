import type { LoginRequest } from '@/objects/auth/request/LoginRequest'
import type { LoginResponse } from '@/objects/auth/response/LoginResponse'
import {
  fromLoginResponseContract,
  toLoginRequestContract,
} from '@/apis/auth/codecs/AuthHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson('/api/auth/login', fromLoginResponseContract, toLoginRequestContract(request))
}
