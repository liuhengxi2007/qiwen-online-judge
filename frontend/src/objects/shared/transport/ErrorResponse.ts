import type { ApiMessageParams } from '@/objects/shared/ApiMessageParams'

/** API 错误响应；code/message 可为空，params 用于国际化或模板化错误文案。 */
export type ErrorResponse = {
  code: string | null
  message: string | null
  params: ApiMessageParams
}
