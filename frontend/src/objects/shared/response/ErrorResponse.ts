import type { ApiMessageParams } from '@/objects/shared/ApiMessageParams'
import { fromApiMessageParamsContract } from '@/objects/shared/ApiMessageParams'
import { readNullable, readRecord, readString } from '@/objects/shared/PageResponse'

export type ErrorResponse = {
  code: string | null
  message: string | null
  params: ApiMessageParams
}

export function fromErrorResponseContract(value: unknown, label: string): ErrorResponse {
  const response = readRecord(value, label)
  return {
    code: readNullable(response.code, `${label} code`, readString),
    message: readNullable(response.message, `${label} message`, readString),
    params: fromApiMessageParamsContract(response.params, `${label} params`),
  }
}
