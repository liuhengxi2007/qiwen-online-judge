import type { ApiMessageParam } from '@/objects/shared/ApiMessageParam'

/** API 消息参数字典；key 由后端消息模板约定，value 限定为可安全序列化的参数值。 */
export type ApiMessageParams = Record<string, ApiMessageParam>
