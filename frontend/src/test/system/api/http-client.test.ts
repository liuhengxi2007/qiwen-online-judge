import { describe, expect, it, vi } from 'vitest'

import type { SuccessResponse } from '@/objects/shared/response/SuccessResponse'
import type { HttpClientError } from '@/system/api/http-client'
import { requestJson } from '@/system/api/http-client'

type ExampleResponse = {
  id: string
  count: number
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json',
    },
  })
}

function textResponse(body: string, status: number): Response {
  return new Response(body, { status })
}

function mockFetch(response: Response): void {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

describe('http client', () => {
  it('passes JSON through as the declared response type', async () => {
    const payload = { id: 'alpha', count: 2 }
    mockFetch(jsonResponse(payload))

    await expect(requestJson<ExampleResponse>('/api/example')).resolves.toEqual(payload)
  })

  it('returns undefined for empty successful responses', async () => {
    mockFetch(new Response(null, { status: 204 }))

    await expect(requestJson<void>('/api/no-content')).resolves.toBeUndefined()
  })

  it('normalizes translated success message envelopes', async () => {
    mockFetch(jsonResponse({ code: 'api.success.problem.deleted', message: null, params: {} }))

    await expect(requestJson<SuccessResponse>('/api/problems/two-sum')).resolves.toEqual({
      code: 'api.success.problem.deleted',
      message: 'Problem deleted.',
      params: {},
    })
  })

  it.each([
    [401, 'unauthorized', { code: 'api.error.auth.required', message: null, params: {} }, 'Authentication required.'],
    [403, 'forbidden', { code: null, message: 'No access.', params: {} }, 'No access.'],
    [404, 'not-found', 'Missing.', 'Missing.'],
    [500, 'http', '', 'Request failed. (HTTP 500)'],
  ] as const)('maps HTTP %s responses to client errors', async (status, kind, body, message) => {
    mockFetch(typeof body === 'string' ? textResponse(body, status) : jsonResponse(body, status))

    await expect(requestJson<unknown>('/api/failure')).rejects.toMatchObject({
      kind,
      message,
    } satisfies Partial<HttpClientError>)
  })
})
