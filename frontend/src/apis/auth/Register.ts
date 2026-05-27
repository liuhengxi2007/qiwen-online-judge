import type { RegisterRequest } from '@/objects/auth/request/RegisterRequest'
import type { RegisterResponse } from '@/objects/auth/response/RegisterResponse'
import {
  fromRegisterResponseContract,
  toRegisterRequestContract,
} from '@/apis/auth/codecs/AuthHttpCodecs'
import { postJson } from '@/system/api/http-client'

export async function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson('/api/auth/register', fromRegisterResponseContract, toRegisterRequestContract(request))
}
