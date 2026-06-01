import type { ApiMessageParams } from '@/objects/shared/ApiMessageParams'

export type SuccessResponse = {
  code: string | null
  message: string | null
  params: ApiMessageParams
}