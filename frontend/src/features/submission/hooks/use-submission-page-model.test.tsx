import { act, renderHook, waitFor } from '@testing-library/react'
import { useEffect } from 'react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'

import type { SubmissionListRequest } from '@/features/submission/http/request/SubmissionListRequest'
import type { SubmissionListResponse } from '@/features/submission/http/response/SubmissionListResponse'
import { useSubmissionPageModel } from '@/features/submission/hooks/use-submission-page-model'

const queryState = vi.hoisted(() => ({
  implementation: (request: SubmissionListRequest) => {
    void request
    return {
      response: {
        items: [] as SubmissionListResponse['items'],
        page: 1,
        pageSize: 10,
        totalItems: 0,
      },
      isLoading: false,
      errorMessage: '',
    }
  },
}))

vi.mock('@/features/submission/hooks/use-submission-list-query', () => ({
  useSubmissionListQuery: (request: SubmissionListRequest) => queryState.implementation(request),
}))

vi.mock('@/features/problem/http/api/ListProblemSuggestions', () => ({
  listProblemSuggestions: vi.fn(),
}))

vi.mock('@/features/user/http/api/ListUserSuggestions', () => ({
  listUserSuggestions: vi.fn(),
}))

let currentSearch = ''

function LocationCapture() {
  const location = useLocation()
  useEffect(() => {
    currentSearch = location.search
  }, [location.search])
  return null
}

function wrapperFor(initialEntry: string) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MemoryRouter initialEntries={[initialEntry]}>
        <LocationCapture />
        {children}
      </MemoryRouter>
    )
  }
}

function responseFor(request: SubmissionListRequest, overrides: Partial<SubmissionListResponse> = {}) {
  return {
    items: [] as SubmissionListResponse['items'],
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
    totalItems: 30,
    ...overrides,
  }
}

describe('useSubmissionPageModel pagination', () => {
  beforeEach(() => {
    currentSearch = ''
    queryState.implementation = (request) => ({
      response: responseFor(request),
      isLoading: false,
      errorMessage: '',
    })
  })

  it('keeps the requested page while the next page is loading', () => {
    queryState.implementation = (request) => ({
      response:
        request.pageRequest.page === 2
          ? responseFor(request, { totalItems: 0 })
          : responseFor(request),
      isLoading: request.pageRequest.page === 2,
      errorMessage: '',
    })

    const rendered = renderHook(() => useSubmissionPageModel(), {
      wrapper: wrapperFor('/submissions'),
      reactStrictMode: false,
    })

    act(() => {
      rendered.result.current.goToPage(2)
    })

    expect(currentSearch).toBe('?page=2')
  })

  it('corrects an out-of-range page after submissions finish loading', async () => {
    queryState.implementation = (request) => ({
      response: responseFor(request, { totalItems: 45 }),
      isLoading: false,
      errorMessage: '',
    })

    renderHook(() => useSubmissionPageModel(), {
      wrapper: wrapperFor('/submissions?page=9'),
      reactStrictMode: false,
    })

    await waitFor(() => {
      expect(currentSearch).toBe('?page=5')
    })
  })
})
