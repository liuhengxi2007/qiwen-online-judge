import { useEffect, useReducer } from 'react'

import { GetContestProblem } from '@/apis/contest/GetContestProblem'
import { GetProblem } from '@/apis/problem/GetProblem'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'

/**
 * 题目详情查询状态，保存题目、加载标记和用户可见错误。
 */
type ProblemDetailQueryState = {
  problem: ProblemDetail | null
  isLoading: boolean
  errorMessage: string
}

/**
 * 题目详情 reducer 动作，覆盖加载、替换和失败状态。
 */
type ProblemDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problem: ProblemDetail }
  | { type: 'replace'; problem: ProblemDetail }
  | { type: 'load_failed'; message: string }

const initialProblemDetailQueryState: ProblemDetailQueryState = {
  problem: null,
  isLoading: true,
  errorMessage: '',
}

/**
 * 题目详情查询 reducer；纯函数维护加载状态，不直接触发网络请求。
 */
function reduceProblemDetailQueryState(
  state: ProblemDetailQueryState,
  action: ProblemDetailQueryAction,
): ProblemDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { problem: action.problem, isLoading: false, errorMessage: '' }
    case 'replace':
      return { problem: action.problem, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { problem: null, isLoading: false, errorMessage: action.message }
  }
}

/**
 * 加载普通或比赛内题目详情；会根据 contestSlug 选择 API，并返回替换题目详情的回调。
 */
export function useProblemDetailQuery(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const [state, dispatch] = useReducer(reduceProblemDetailQueryState, initialProblemDetailQueryState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    const message = contestSlug ? new GetContestProblem(contestSlug, problemSlug) : new GetProblem(problemSlug)
    void sendAPI(message)
      .then((problem) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problem })
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return
        }
        dispatch({
          type: 'load_failed',
          message:
            // 注意：公开题目详情把 forbidden 和 not-found 都展示为 404，用于隐藏受保护题目是否存在。
            isHttpClientError(error) && (error.kind === 'not-found' || error.kind === 'forbidden')
              ? '404 Not Found.'
              : 'Unable to load problem details.',
        })
      })

    return () => {
      cancelled = true
    }
  }, [contestSlug, problemSlug])

  function replaceProblem(problem: ProblemDetail) {
    dispatch({ type: 'replace', problem })
  }

  return {
    ...state,
    replaceProblem,
  }
}
