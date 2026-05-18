import type { ApiMessageParam } from '@/shared/model/ApiMessageParam'

export type ErrorResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}
