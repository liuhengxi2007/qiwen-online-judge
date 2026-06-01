import type { ApiMessageParams } from '@/objects/shared/ApiMessageParams'

export type ErrorResponse = {
  code: string | null
  message: string | null
  params: ApiMessageParams
}