export type HttpClientErrorKind = 'unauthorized' | 'forbidden' | 'not-found' | 'http'
export type JsonDecoder<T> = {
  bivarianceHack(value: unknown): T
}['bivarianceHack']

type ErrorResponse = {
  message?: string
}

type SuccessResponse = {
  message: string
}

export class HttpClientError extends Error {
  readonly kind: HttpClientErrorKind

  constructor(kind: HttpClientErrorKind, message: string) {
    super(message)
    this.kind = kind
  }
}

async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const jsonResponse = response.clone()
  const textResponse = response.clone()
  const errorData = parseErrorResponse(await jsonResponse.json().catch(() => null))

  if (errorData?.message) {
    return errorData.message
  }

  const text = (await textResponse.text().catch(() => '')).trim()
  if (text) {
    return text
  }

  return `${fallback} (HTTP ${response.status})`
}

function parseErrorResponse(value: unknown): ErrorResponse | null {
  if (!isRecord(value)) {
    return null
  }

  return typeof value.message === 'string' ? { message: value.message } : null
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function decodeSuccessResponse(value: unknown): SuccessResponse {
  if (!isRecord(value) || typeof value.message !== 'string') {
    throw new Error('Invalid success response payload.')
  }

  return { message: value.message }
}

export async function requestJson<T>(input: RequestInfo, decode: JsonDecoder<T>, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: 'same-origin',
    ...init,
  })

  if (response.status === 401) {
    throw new HttpClientError('unauthorized', await readErrorMessage(response, 'Authentication required.'))
  }

  if (response.status === 403) {
    throw new HttpClientError('forbidden', await readErrorMessage(response, 'Forbidden.'))
  }

  if (response.status === 404) {
    throw new HttpClientError('not-found', await readErrorMessage(response, 'Not found.'))
  }

  if (!response.ok) {
    throw new HttpClientError('http', await readErrorMessage(response, 'Request failed.'))
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
