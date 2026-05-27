import type { ApiMessageParam } from '@/objects/shared/ApiMessageParam'

export type ErrorResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}
