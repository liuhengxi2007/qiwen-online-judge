import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { SubmissionListRequest } from '@/objects/submission/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/objects/submission/response/SubmissionListResponse'
import { useSubmissionListQuery } from './useSubmissionListQuery'

const submissionClient = vi.hoisted(() => ({
  sendAPI: vi.fn(),
}))

vi.mock('@/system/api/api-message', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/system/api/api-message')>()
  return {
    ...actual,
    sendAPI: submissionClient.sendAPI,
  }
})

function emptyResponseFor(request: SubmissionListRequest): SubmissionListResponse {
  return {
    items: [],
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
    totalItems: 0,
  }
}

describe('useSubmissionListQuery', () => {
  beforeEach(() => {
    submissionClient.sendAPI.mockReset()
    submissionClient.sendAPI.mockImplementation((message: { apiPath: string }) => {
      const url = new URL(`http://example.test/${message.apiPath}`)
      return Promise.resolve(
        emptyResponseFor({
          userQuery: null,
          problemQuery: null,
          verdict: 'all',
          sort: 'submitted',
          direction: 'desc',
          pageRequest: {
            page: Number(url.searchParams.get('page') ?? 1),
            pageSize: Number(url.searchParams.get('pageSize') ?? 10),
          },
        }),
      )
    })
  })

  it('loads submissions with the nested page request preserved from the request key', async () => {
    const request: SubmissionListRequest = {
      userQuery: null,
      problemQuery: null,
      verdict: 'all',
      sort: 'submitted',
      direction: 'desc',
      pageRequest: {
        page: 3,
        pageSize: 25,
      },
    }

    renderHook(() => useSubmissionListQuery(request), { reactStrictMode: false })

    await waitFor(() => {
      expect(submissionClient.sendAPI).toHaveBeenCalledWith(
        expect.objectContaining({
          apiPath: 'submissions?verdict=all&sort=submitted&direction=desc&page=3&pageSize=25',
        }),
      )
    })
  })
})
