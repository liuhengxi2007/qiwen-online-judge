import type { ApiMessageParam } from '@contracts/shared'

export type SuccessResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}
