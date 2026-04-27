import { useEffect, useState } from 'react'

import { listProblems } from '@/features/problem/api/problem-client'
import type { ProblemListRequest, ProblemListResponse } from '@/features/problem/domain/problem'
import { translateMessage } from '@/shared/i18n/messages'

function requestKey(request: ProblemListRequest): string {
  return JSON.stringify(request)
}

export function useProblemPageModel(request: ProblemListRequest) {
  const key = requestKey(request)
  const [state, setState] = useState<{
    key: string
    response: ProblemListResponse
    isLoading: boolean
    errorMessage: string
  }>({
    key: '',
    response: {
      items: [],
      page: request.pageRequest.page,
      pageSize: request.pageRequest.pageSize,
      totalItems: 0,
    },
    isLoading: true,
    errorMessage: '',
  })

  useEffect(() => {
    let cancelled = false
    setState((currentState) => ({
      ...currentState,
      isLoading: true,
      errorMessage: '',
    }))
    void listProblems(request)
      .then((response) => {
        if (cancelled) {
          return
        }
        setState({
          key,
          response,
          isLoading: false,
          errorMessage: '',
        })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        setState({
          key,
          response: {
            items: [],
            page: request.pageRequest.page,
            pageSize: request.pageRequest.pageSize,
            totalItems: 0,
          },
          isLoading: false,
          errorMessage: translateMessage('problem.list.loadFailed'),
        })
      })

    return () => {
      cancelled = true
    }
  }, [key])

  return {
    problems: state.key === key ? state.response.items : [],
    page: state.key === key ? state.response.page : request.pageRequest.page,
    pageSize: state.key === key ? state.response.pageSize : request.pageRequest.pageSize,
    totalItems: state.key === key ? state.response.totalItems : 0,
    isLoading: state.isLoading || state.key !== key,
    errorMessage: state.key === key ? state.errorMessage : '',
  }
}
