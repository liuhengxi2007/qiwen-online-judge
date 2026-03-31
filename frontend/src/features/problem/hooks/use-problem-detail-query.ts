import { useEffect, useReducer } from 'react'

import { getProblem } from '@/features/problem/api/problem-client'
import type { ProblemDetail, ProblemSlug } from '@/features/problem/domain/problem'

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

const initialState: ProblemDetailQueryState = {
  problem: null,
  isLoading: true,
  errorMessage: '',
}

function reducer(state: ProblemDetailQueryState, action: ProblemDetailQueryAction): ProblemDetailQueryState {
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

export function useProblemDetailQuery(problemSlug: ProblemSlug) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void getProblem(problemSlug)
      .then((problem) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problem })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load problem details.' })
      })

    return () => {
      cancelled = true
    }
  }, [problemSlug])

  function replaceProblem(problem: ProblemDetail) {
    dispatch({ type: 'replace', problem })
  }

  return {
    ...state,
    replaceProblem,
  }
}
