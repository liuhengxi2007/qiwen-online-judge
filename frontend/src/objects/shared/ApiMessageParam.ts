export type ApiMessageParam =
  | { kind: 'text'; value: string }
  | { kind: 'int'; value: number }
  | { kind: 'long'; value: number }
  | { kind: 'bool'; value: boolean }

export function fromApiMessageParamContract(value: unknown, label: string): ApiMessageParam {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  const param = value as Record<string, unknown>
  switch (param.kind) {
    case 'text':
      if (typeof param.value !== 'string') {
        throw new Error(`Invalid ${label} value in contract payload: expected string.`)
      }
      return { kind: 'text', value: param.value }
    case 'int':
    case 'long':
      if (typeof param.value !== 'number' || !Number.isSafeInteger(param.value)) {
        throw new Error(`Invalid ${label} value in contract payload: expected safe integer.`)
      }
      return { kind: param.kind, value: param.value }
    case 'bool':
      if (typeof param.value !== 'boolean') {
        throw new Error(`Invalid ${label} value in contract payload: expected boolean.`)
      }
      return { kind: 'bool', value: param.value }
    default:
      throw new Error(`Invalid ${label} kind in contract payload.`)
  }
}
