import { requestJson } from '@/system/api/http-client'

export type APIMethod = 'GET' | 'POST'

export type APIMessage<Response> = {
  readonly responseType?: Response
  readonly apiPath: string
  readonly method: APIMethod
  body(): unknown | undefined
}

export type APIWithSessionMessage<Response> = APIMessage<Response>

export function apiPath(message: APIMessage<unknown>): string {
  const normalizedPath = message.apiPath.replace(/^\/+/, '')
  return normalizedPath.startsWith('api/') ? `/${normalizedPath}` : `/api/${normalizedPath}`
}

export function apiRequest(message: APIMessage<unknown>): RequestInit {
  const body = message.body()
  if (body === undefined) {
    return {
      method: message.method,
      credentials: 'same-origin',
    }
  }

  return {
    method: message.method,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  }
}

export function sendAPI<Response>(message: APIMessage<Response>): Promise<Response> {
  return requestJson(apiPath(message), apiRequest(message))
}

export function sendMultipartAPI<Response>(
  message: APIMessage<Response>,
  body: FormData,
): Promise<Response> {
  return requestJson(apiPath(message), {
    method: message.method,
    credentials: 'same-origin',
    body,
  })
}
