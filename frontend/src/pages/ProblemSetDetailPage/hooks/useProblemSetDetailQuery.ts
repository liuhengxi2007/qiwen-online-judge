import { useEffect, useReducer } from 'react'

import { GetProblemSet } from '@/apis/problemset/GetProblemSet'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'

/**
 * 题单详情查询状态，保存详情、加载标记和错误消息。
 */
type ProblemSetDetailQueryState = {
  problemSet: ProblemSetDetail | null
  isLoading: boolean
  errorMessage: string
}

/**
 * 题单详情 reducer 动作，覆盖加载、替换和失败状态。
 */
type ProblemSetDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problemSet: ProblemSetDetail }
  | { type: 'replace'; problemSet: ProblemSetDetail }
  | { type: 'load_failed'; message: string }

const initialState: ProblemSetDetailQueryState = {
  problemSet: null,
  isLoading: true,
  errorMessage: '',
}

/**
 * 题单详情查询 reducer；纯函数维护详情状态。
 */
function reducer(state: ProblemSetDetailQueryState, action: ProblemSetDetailQueryAction): ProblemSetDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { problemSet: action.problemSet, isLoading: false, errorMessage: '' }
    case 'replace':
      return { problemSet: action.problemSet, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { problemSet: null, isLoading: false, errorMessage: action.message }
  }
}

/**
 * 题单详情查询 hook；加载题单详情并提供替换详情回调。
 */
export function useProblemSetDetailQuery(problemSetSlug: ProblemSetSlug) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void sendAPI(new GetProblemSet(problemSetSlug))
      .then((problemSet) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problemSet })
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return
        }
        dispatch({
          type: 'load_failed',
          message:
            // 注意：题单详情把 forbidden 和 not-found 都展示为 404，用于隐藏受保护题单是否存在。
            isHttpClientError(error) && (error.kind === 'not-found' || error.kind === 'forbidden')
              ? '404 Not Found.'
              : 'Unable to load problem set details.',
        })
      })

    return () => {
      cancelled = true
    }
  }, [problemSetSlug])

  function replaceProblemSet(problemSet: ProblemSetDetail) {
    dispatch({ type: 'replace', problemSet })
  }

  return {
    ...state,
    replaceProblemSet,
  }
}
