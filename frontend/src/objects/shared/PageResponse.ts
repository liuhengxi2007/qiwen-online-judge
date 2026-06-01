export type PageResponse<TItem> = {
  items: TItem[]
  page: number
  pageSize: number
  totalItems: number
}

type ContractDecoder<T> = (value: unknown, label: string) => T

export function readRecord(value: unknown, label: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected object.`)
  }

  return value as Record<string, unknown>
}

export function readString(value: unknown, label: string): string {
  if (typeof value !== 'string') {
    throw new Error(`Invalid ${label} in contract payload: expected string.`)
  }

  return value
}

export function readBoolean(value: unknown, label: string): boolean {
  if (typeof value !== 'boolean') {
    throw new Error(`Invalid ${label} in contract payload: expected boolean.`)
  }

  return value
}

export function readNumber(value: unknown, label: string): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected finite number.`)
  }

  return value
}

export function readSafeInteger(value: unknown, label: string): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected safe integer.`)
  }

  return value
}

export function readPositiveSafeInteger(value: unknown, label: string): number {
  const integer = readSafeInteger(value, label)
  if (integer <= 0) {
    throw new Error(`Invalid ${label} in contract payload: expected positive safe integer.`)
  }

  return integer
}

export function readNonNegativeSafeInteger(value: unknown, label: string): number {
  const integer = readSafeInteger(value, label)
  if (integer < 0) {
    throw new Error(`Invalid ${label} in contract payload: expected non-negative safe integer.`)
  }

  return integer
}

export function readNullable<T>(
  value: unknown,
  label: string,
  decode: ContractDecoder<T>,
): T | null {
  return value === null ? null : decode(value, label)
}

export function readArray<T>(
  value: unknown,
  label: string,
  decode: ContractDecoder<T>,
): T[] {
  if (!Array.isArray(value)) {
    throw new Error(`Invalid ${label} in contract payload: expected array.`)
  }

  return value.map((item, index) => decode(item, `${label} ${index}`))
}

export function fromPageResponseContract<TItem>(
  value: unknown,
  label: string,
  decodeItem: ContractDecoder<TItem>,
): PageResponse<TItem> {
  const response = readRecord(value, label)
  return {
    items: readArray(response.items, `${label} items`, decodeItem),
    page: readPositiveSafeInteger(response.page, `${label} page`),
    pageSize: readPositiveSafeInteger(response.pageSize, `${label} page size`),
    totalItems: readNonNegativeSafeInteger(response.totalItems, `${label} total items`),
  }
}
