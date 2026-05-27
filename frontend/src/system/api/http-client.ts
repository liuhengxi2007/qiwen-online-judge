import { messages, fallbackLocale, resolveLocale, translateMessage } from '@/system/i18n/messages'
import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { ApiMessageParam } from '@/objects/shared/ApiMessageParam'

export type HttpClientErrorKind = 'unauthorized' | 'forbidden' | 'not-found' | 'http'
export type JsonDecoder<T> = {
  bivarianceHack(value: unknown): T
}['bivarianceHack']

type ApiMessageResponse = {
  code?: string
  message?: string
  params?: Record<string, ApiMessageParam>
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
  const errorData = parseApiMessageResponse(await jsonResponse.json().catch(() => null))

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

function parseApiMessageResponse(value: unknown): ApiMessageResponse | null {
  if (!isRecord(value)) {
    return null
  }

  const code = typeof value.code === 'string' ? value.code : undefined
  const message = typeof value.message === 'string' ? value.message : undefined
  const params =
    isRecord(value.params) && Object.values(value.params).every(isApiMessageParam)
      ? (value.params as Record<string, ApiMessageParam>)
      : undefined

  if (!code && !message) {
    return null
  }

  return { code, message, params }
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
      return typeof value.value === 'number'
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
  const data = parseApiMessageResponse(value)
  if (!data) {
    throw new Error(translateMessage('common.error.invalidSuccessPayload'))
  }

  return {
    message: translateApiMessage(data) ?? data.message ?? translateMessage('common.success.generic'),
    code: data.code ?? null,
    params: data.params ?? {},
  }
}

function hasTranslation(key: string): boolean {
  const locale = resolveLocale()
  return key in messages[locale] || key in messages[fallbackLocale]
}

function translateApiMessage(data: ApiMessageResponse): string | null {
  if (data.code && hasTranslation(data.code)) {
    return translateMessage(data.code, toTranslationParams(data.params ?? {}))
  }

  if (data.message) {
    return data.message
  }

  return null
}

export async function requestJson<T>(input: RequestInfo, decode: JsonDecoder<T>, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: 'same-origin',
    ...init,
  })

  if (response.status === 401) {
    const message = await readErrorMessage(response, translateMessage('common.error.authRequired'))
    const data = parseApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('unauthorized', message, data?.code, data?.params)
  }

  if (response.status === 403) {
    const message = await readErrorMessage(response, translateMessage('common.error.forbidden'))
    const data = parseApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('forbidden', message, data?.code, data?.params)
  }

  if (response.status === 404) {
    const message = await readErrorMessage(response, translateMessage('common.error.notFound'))
    const data = parseApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('not-found', message, data?.code, data?.params)
  }

  if (!response.ok) {
    const message = await readErrorMessage(response, translateMessage('common.error.requestFailed'))
    const data = parseApiMessageResponse(await response.clone().json().catch(() => null))
    throw new HttpClientError('http', message, data?.code, data?.params)
  }

  return decode(await response.json())
}

export function postJson<TResponse>(input: RequestInfo, decode: JsonDecoder<TResponse>, body?: unknown): Promise<TResponse> {
  return requestJson<TResponse>(input, decode, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export function postMultipart<TResponse>(
  input: RequestInfo,
  decode: JsonDecoder<TResponse>,
  body: FormData,
): Promise<TResponse> {
  return requestJson<TResponse>(input, decode, {
    method: 'POST',
    body,
  })
}
