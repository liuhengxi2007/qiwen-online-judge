export type HttpClientErrorKind = 'unauthorized' | 'forbidden' | 'not-found' | 'http'

type ErrorResponse = {
  message?: string
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
  const errorData = (await jsonResponse.json().catch(() => null)) as ErrorResponse | null

  if (errorData?.message) {
    return errorData.message
  }

  const text = (await textResponse.text().catch(() => '')).trim()
  if (text) {
    return text
  }

  return `${fallback} (HTTP ${response.status})`
}

export async function requestJson<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
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

  return (await response.json()) as T
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
