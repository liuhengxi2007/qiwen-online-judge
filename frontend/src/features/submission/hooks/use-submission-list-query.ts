import { useEffect, useState } from 'react'

import { listSubmissions } from '@/features/submission/api/submission-client'
import {
  isTerminalSubmissionStatus,
  type SubmissionListRequest,
  type SubmissionListResponse,
} from '@/features/submission/domain/submission'

function requestKey(request: SubmissionListRequest): string {
  return JSON.stringify({
    userQuery: request.userQuery,
    problemQuery: request.problemQuery,
    verdict: request.verdict,
    sort: request.sort,
    direction: request.direction,
    page: request.pageRequest.page,
    pageSize: request.pageRequest.pageSize,
  })
}

export function useSubmissionListQuery(request: SubmissionListRequest) {
  const key = requestKey(request)
  const [queryState, setQueryState] = useState<{
    key: string
    response: SubmissionListResponse
    errorMessage: string
  }>({
    key: '',
    response: {
      items: [],
      page: request.pageRequest.page,
      pageSize: request.pageRequest.pageSize,
      totalItems: 0,
    },
    errorMessage: '',
  })

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null

    const load = () => {
      void listSubmissions(request)
        .then((loadedResponse) => {
          if (cancelled) {
            return
          }

          setQueryState({
            key,
            response: loadedResponse,
            errorMessage: '',
          })
          if (!loadedResponse.items.some((submission) => !isTerminalSubmissionStatus(submission.status)) && intervalId !== null) {
            window.clearInterval(intervalId)
            intervalId = null
          }
        })
        .catch(() => {
          if (cancelled) {
            return
          }

          setQueryState({
            key,
            response: {
              items: [],
              page: request.pageRequest.page,
              pageSize: request.pageRequest.pageSize,
              totalItems: 0,
            },
            errorMessage: 'Unable to load submissions.',
          })
        })
    }

    load()
    intervalId = window.setInterval(load, 3000)

    return () => {
      cancelled = true
      if (intervalId !== null) {
        window.clearInterval(intervalId)
      }
    }
  }, [key])

  return {
    response:
      queryState.key === key
        ? queryState.response
        : {
            items: [],
            page: request.pageRequest.page,
            pageSize: request.pageRequest.pageSize,
            totalItems: 0,
          },
    isLoading: queryState.key !== key,
    errorMessage: queryState.key === key ? queryState.errorMessage : '',
  }
}
