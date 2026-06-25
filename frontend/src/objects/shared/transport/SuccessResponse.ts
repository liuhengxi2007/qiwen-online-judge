import type { ApiMessageParams } from '@/objects/shared/ApiMessageParams'

/** API 成功响应；用于无业务实体返回的命令结果，可附带提示消息参数。 */
export type SuccessResponse = {
  code: string | null
  message: string | null
  params: ApiMessageParams
}
