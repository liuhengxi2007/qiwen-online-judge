import type { RegisterRequest } from '@/features/auth/http/request/RegisterRequest'
import type { RegisterResponse } from '@/features/auth/http/response/RegisterResponse'
import {
  fromRegisterResponseContract,
  toRegisterRequestContract,
} from '@/features/auth/http/codec/AuthHttpCodecs'
import { postJson } from '@/shared/api/http-client'

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson('/api/auth/register', fromRegisterResponseContract, toRegisterRequestContract(request))
}
