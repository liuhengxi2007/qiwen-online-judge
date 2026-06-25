import { useEffect, useReducer } from 'react'

import { GetContestProblem } from '@/apis/contest/GetContestProblem'
import { GetProblem } from '@/apis/problem/GetProblem'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { problemSubmitDetailFromProblem, type ProblemSubmitDetail } from '../objects/ProblemSubmitDetail'

type ProblemSubmitDetailQueryState = {
  problem: ProblemSubmitDetail | null
  isLoading: boolean
  errorMessage: string
}

type ProblemSubmitDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problem: ProblemSubmitDetail }
  | { type: 'load_failed'; message: string }

const initialProblemSubmitDetailQueryState: ProblemSubmitDetailQueryState = {
  problem: null,
  isLoading: true,
  errorMessage: '',
}

function reduceProblemSubmitDetailQueryState(
  state: ProblemSubmitDetailQueryState,
  action: ProblemSubmitDetailQueryAction,
): ProblemSubmitDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { problem: action.problem, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { problem: null, isLoading: false, errorMessage: action.message }
  }
}

/**
 * 加载提交页头部需要的题目详情，并丢弃提交页不使用的题面字段。
 */
export function useProblemSubmitDetailQuery(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const [state, dispatch] = useReducer(reduceProblemSubmitDetailQueryState, initialProblemSubmitDetailQueryState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    const message = contestSlug ? new GetContestProblem(contestSlug, problemSlug) : new GetProblem(problemSlug)
    void sendAPI(message)
      .then((problem) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problem: problemSubmitDetailFromProblem(problem) })
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

  return state
}

export type ProblemSubmitDetailQuery = ReturnType<typeof useProblemSubmitDetailQuery>
