import type { ApiMessageParam } from '@/objects/shared/ApiMessageParam'
import { fromApiMessageParamContract } from '@/objects/shared/ApiMessageParam'
import { readRecord } from '@/objects/shared/PageResponse'

export type ApiMessageParams = Record<string, ApiMessageParam>

export function fromApiMessageParamsContract(value: unknown, label: string): ApiMessageParams {
  const params = readRecord(value, label)
  return Object.fromEntries(
    Object.entries(params).map(([key, param]) => [key, fromApiMessageParamContract(param, `${label} ${key}`)]),
  )
}
