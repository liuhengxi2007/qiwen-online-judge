import { useEffect, useReducer } from 'react'

import { GetContestProblem } from '@/apis/contest/GetContestProblem'
import { GetProblem } from '@/apis/problem/GetProblem'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { problemDataDetailFromProblem, type ProblemDataDetail } from '../objects/ProblemDataDetail'

type ProblemDataDetailQueryState = {
  problem: ProblemDataDetail | null
  isLoading: boolean
  errorMessage: string
}

type ProblemDataDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problem: ProblemDataDetail }
  | { type: 'replace'; problem: ProblemDataDetail }
  | { type: 'load_failed'; message: string }

const initialProblemDataDetailQueryState: ProblemDataDetailQueryState = {
  problem: null,
  isLoading: true,
  errorMessage: '',
}

function reduceProblemDataDetailQueryState(
  state: ProblemDataDetailQueryState,
  action: ProblemDataDetailQueryAction,
): ProblemDataDetailQueryState {
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
 * 加载测试数据页需要的题目详情，并把完整响应收窄为数据页字段。
 */
export function useProblemDataDetailQuery(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const [state, dispatch] = useReducer(reduceProblemDataDetailQueryState, initialProblemDataDetailQueryState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    const message = contestSlug ? new GetContestProblem(contestSlug, problemSlug) : new GetProblem(problemSlug)
    void sendAPI(message)
      .then((problem) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problem: problemDataDetailFromProblem(problem) })
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
    dispatch({ type: 'replace', problem: problemDataDetailFromProblem(problem) })
  }

  return {
    ...state,
    replaceProblem,
  }
}
