import { describe, expect, it } from 'vitest'

import { fromPageResponseContract } from '@/objects/shared/PageResponse'
import { fromSubmissionListResponseContract } from '@/objects/submission/response/SubmissionListResponse'
import { decodeSuccessResponse } from '@/system/api/http-client'

const baseSubmission = {
  id: 1,
  problemId: '11111111-1111-4111-8111-111111111111',
  problemSlug: 'two-sum',
  problemTitle: 'Two Sum',
  canViewDetail: true,
  submitter: {
    username: 'alice',
    displayName: 'Alice',
  },
  language: 'cpp17',
  status: 'queued',
  verdict: null,
  timeUsedMs: null,
  memoryUsedKb: null,
  score: null,
  codeLength: 10,
  submittedAt: '2026-01-01T00:00:00Z',
  startedAt: null,
  finishedAt: null,
}

describe('api decoders', () => {
  it('rejects unsafe page totals', () => {
    expect(() =>
      fromPageResponseContract(
        {
          items: [],
          page: 1,
          pageSize: 10,
          totalItems: Number.MAX_SAFE_INTEGER + 1,
        },
        'page response',
        (value) => value,
      ),
    ).toThrow('Invalid page response total items in contract payload')
  })

  it('rejects malformed submission list items', () => {
    expect(() =>
      fromSubmissionListResponseContract({
        items: [{ ...baseSubmission, status: 'finished' }],
        page: 1,
        pageSize: 10,
        totalItems: 1,
      }),
    ).toThrow('Invalid submission status in contract payload')
  })

  it('rejects unsafe long message params', () => {
    expect(() =>
      decodeSuccessResponse({
        code: null,
        message: 'ok',
        params: {
          total: { kind: 'long', value: Number.MAX_SAFE_INTEGER + 1 },
        },
      }),
    ).toThrow('Invalid success response params total value in contract payload')
  })

  it('rejects unsafe long-like fields before UI state', () => {
    expect(() =>
      fromSubmissionListResponseContract({
        items: [{ ...baseSubmission, codeLength: Number.MAX_SAFE_INTEGER + 1 }],
        page: 1,
        pageSize: 10,
        totalItems: 1,
      }),
    ).toThrow('Invalid submission list response items 0 code length in contract payload')
  })
})
