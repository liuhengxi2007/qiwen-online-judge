import { useEffect, useReducer } from 'react'

import { GetContestProblem } from '@/apis/contest/GetContestProblem'
import { GetProblem } from '@/apis/problem/GetProblem'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'

type ProblemDetailQueryState = {
  problem: ProblemDetail | null
  isLoading: boolean
  errorMessage: string
}

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
            error instanceof HttpClientError && (error.kind === 'not-found' || error.kind === 'forbidden')
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
