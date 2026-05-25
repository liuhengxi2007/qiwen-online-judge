import { useEffect, useReducer } from 'react'

import { getProblemSet } from '@/features/problemset/http/api/GetProblemSet'
import type { ProblemSetDetail } from '@/features/problemset/model/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/features/problemset/model/ProblemSetSlug'
import { HttpClientError } from '@/shared/api/http-client'

type ProblemSetDetailQueryState = {
  problemSet: ProblemSetDetail | null
  isLoading: boolean
  errorMessage: string
}

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

export function useProblemSetDetailQuery(problemSetSlug: ProblemSetSlug) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void getProblemSet(problemSetSlug)
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
            error instanceof HttpClientError && (error.kind === 'not-found' || error.kind === 'forbidden')
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
