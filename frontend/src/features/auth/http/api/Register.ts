import type {
  RegisterRequest,
  RegisterResponse,
} from '@/features/auth/domain/auth'
import {
  fromRegisterResponseContract,
  toRegisterRequestContract,
} from '@/features/auth/domain/auth'
import { postJson } from '@/shared/api/http-client'

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson('/api/auth/register', fromRegisterResponseContract, toRegisterRequestContract(request))
}
