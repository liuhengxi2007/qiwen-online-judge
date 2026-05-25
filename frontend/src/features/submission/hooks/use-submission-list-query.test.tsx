import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { SubmissionListRequest } from '@/features/submission/http/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/features/submission/http/response/SubmissionListResponse'
import { useSubmissionListQuery } from '@/features/submission/hooks/use-submission-list-query'

const submissionClient = vi.hoisted(() => ({
  getSubmission: vi.fn(),
  listSubmissions: vi.fn(),
}))

vi.mock('@/features/submission/http/api/GetSubmission', () => ({
  getSubmission: submissionClient.getSubmission,
}))

vi.mock('@/features/submission/http/api/ListSubmissions', () => ({
  listSubmissions: submissionClient.listSubmissions,
}))

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
    submissionClient.getSubmission.mockReset()
    submissionClient.listSubmissions.mockReset()
    submissionClient.listSubmissions.mockImplementation((request: SubmissionListRequest) =>
      Promise.resolve(emptyResponseFor(request)),
    )
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
      expect(submissionClient.listSubmissions).toHaveBeenCalledWith(request)
    })
  })
})
