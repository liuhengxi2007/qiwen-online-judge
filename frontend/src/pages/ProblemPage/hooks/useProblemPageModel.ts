import { useEffect, useState } from 'react'

import { ListProblems } from '@/apis/problem/ListProblems'
import type { ProblemListRequest } from '@/objects/problem/request/ProblemListRequest'
import type { ProblemListResponse } from '@/objects/problem/response/ProblemListResponse'
import { sendAPI } from '@/system/api/api-message'
import { translateMessage } from '@/system/i18n/messages'

/**
 * 为题目列表请求生成稳定 key，确保分页变化时触发重新加载。
 */
function requestKey(request: ProblemListRequest): string {
  return JSON.stringify(request)
}

/**
 * 题目列表模型 hook；按请求加载题目摘要分页并返回加载状态。
 */
export function useProblemPageModel(request: ProblemListRequest) {
  const query = request.query
  const page = request.pageRequest.page
  const pageSize = request.pageRequest.pageSize
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
      page,
      pageSize,
      totalItems: 0,
    },
    isLoading: true,
    errorMessage: '',
  })

  useEffect(() => {
    let cancelled = false
    const nextRequest = { query, pageRequest: { page, pageSize } }
    void sendAPI(new ListProblems(nextRequest))
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
            page,
            pageSize,
            totalItems: 0,
          },
          isLoading: false,
          errorMessage: translateMessage('problem.list.loadFailed'),
        })
      })

    return () => {
      cancelled = true
    }
  }, [key, page, pageSize, query])

  return {
    problems: state.key === key ? state.response.items : [],
    page: state.key === key ? state.response.page : page,
    pageSize: state.key === key ? state.response.pageSize : pageSize,
    totalItems: state.key === key ? state.response.totalItems : 0,
    isLoading: state.isLoading || state.key !== key,
    errorMessage: state.key === key ? state.errorMessage : '',
  }
}
