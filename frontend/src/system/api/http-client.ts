import { messages, fallbackLocale, resolveLocale, translateMessage } from '@/system/i18n/messages'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { ApiMessageParam } from '@/objects/shared/ApiMessageParam'

export type HttpClientErrorKind = 'unauthorized' | 'forbidden' | 'not-found' | 'http'

type ApiMessageResponse = {
  code: string | null
  message: string | null
  params: Record<string, ApiMessageParam>
}

export class HttpClientError extends Error {
  readonly kind: HttpClientErrorKind
  readonly code?: string
  readonly params?: Record<string, ApiMessageParam>

  constructor(kind: HttpClientErrorKind, message: string, code?: string, params?: Record<string, ApiMessageParam>) {
    super(message)
    this.kind = kind
    this.code = code
    this.params = params
  }
}

async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const jsonResponse = response.clone()
  const textResponse = response.clone()
  const errorData = decodeApiMessageResponse(await jsonResponse.json().catch(() => null))

  if (errorData) {
    const translated = translateApiMessage(errorData)
    if (translated) {
      return translated
    }
  }

  const text = (await textResponse.text().catch(() => '')).trim()
  if (text) {
    return text
  }

  return `${fallback} (HTTP ${response.status})`
}

export function decodeApiMessageResponse(value: unknown): ApiMessageResponse | null {
  if (!isRecord(value)) {
    return null
  }

  const code = value.code === null || typeof value.code === 'string' ? value.code : undefined
  const message = value.message === null || typeof value.message === 'string' ? value.message : undefined
  const params =
    isRecord(value.params) && Object.values(value.params).every(isApiMessageParam)
      ? (value.params as Record<string, ApiMessageParam>)
      : undefined

  if (code === undefined || message === undefined || params === undefined) {
    return null
  }

  return { code, message, params }
}

function isExactSuccessResponse(value: unknown): boolean {
  if (!isRecord(value)) {
    return false
  }

  const keys = Object.keys(value).sort()
  return keys.length === 3 && keys[0] === 'code' && keys[1] === 'message' && keys[2] === 'params'
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isApiMessageParam(value: unknown): value is ApiMessageParam {
  if (!isRecord(value) || typeof value.kind !== 'string' || !('value' in value)) {
    return false
  }

  switch (value.kind) {
    case 'text':
      return typeof value.value === 'string'
    case 'int':
    case 'long':
      return typeof value.value === 'number' && Number.isSafeInteger(value.value)
    case 'bool':
      return typeof value.value === 'boolean'
    default:
      return false
  }
}

function toTranslationParams(params: Record<string, ApiMessageParam>): Record<string, string | number> {
  return Object.fromEntries(
    Object.entries(params).map(([key, value]) => {
      switch (value.kind) {
        case 'text':
        case 'int':
        case 'long':
          return [key, value.value]
        case 'bool':
          return [key, String(value.value)]
      }
    }),
  )
}

export function decodeSuccessResponse(value: unknown): SuccessResponse {
  const response = decodeApiMessageResponse(value)
  if (!response || (!response.code && !response.message)) {
    throw new Error(translateMessage('common.error.invalidSuccessPayload'))
  }

  const data: ApiMessageResponse = {
    code: response.code,
    message: response.message,
    params: response.params,
  }

  return {
    message: translateApiMessage(data) ?? data.message ?? translateMessage('common.success.generic'),
    code: response.code,
    params: response.params,
  }
}

function hasTranslation(key: string): boolean {
  const locale = resolveLocale()
  return key in messages[locale] || key in messages[fallbackLocale]
}

function translateApiMessage(data: ApiMessageResponse): string | null {
  if (data.code && hasTranslation(data.code)) {
    return translateMessage(data.code, toTranslationParams(data.params))
  }

  if (data.message) {
    return data.message
  }

  return null
}

export async function requestJson<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: 'same-origin',
    ...init,
  })

  if (response.status === 401) {
    const message = await readErrorMessage(response, translateMessage('common.error.authRequired'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('unauthorized', message, data?.code ?? undefined, data?.params)
  }

  if (response.status === 403) {
    const message = await readErrorMessage(response, translateMessage('common.error.forbidden'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('forbidden', message, data?.code ?? undefined, data?.params)
  }

  if (response.status === 404) {
    const message = await readErrorMessage(response, translateMessage('common.error.notFound'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('not-found', message, data?.code ?? undefined, data?.params)
  }

  if (!response.ok) {
    const message = await readErrorMessage(response, translateMessage('common.error.requestFailed'))
    const data = decodeApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('http', message, data?.code ?? undefined, data?.params)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text.trim()) {
    return undefined as T
  }

  const value = JSON.parse(text) as unknown
  return (isExactSuccessResponse(value) ? decodeSuccessResponse(value) : value) as T
}

export function postJson<TResponse>(input: RequestInfo, body?: unknown): Promise<TResponse> {
  return requestJson<TResponse>(input, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export function postMultipart<TResponse>(
  input: RequestInfo,
  body: FormData,
): Promise<TResponse> {
  return requestJson<TResponse>(input, {
    method: 'POST',
    body,
  })
}
