import type { ApiMessageParam } from '@/shared/model/ApiMessageParam'

export type SuccessResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}
