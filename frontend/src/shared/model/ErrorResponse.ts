import type { ApiMessageParam } from '@contracts/shared'

export type ErrorResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}
