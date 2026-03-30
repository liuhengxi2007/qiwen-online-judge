import { useEffect, useReducer } from 'react'

import { listProblems } from '@/features/problem/api/problem-client'
import type { ProblemSummary } from '@/features/problem/domain/problem'

type ProblemPageState = {
  problems: ProblemSummary[]
  isLoading: boolean
  errorMessage: string
}

type ProblemPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problems: ProblemSummary[] }
  | { type: 'load_failed'; message: string }

const initialState: ProblemPageState = {
  problems: [],
  isLoading: true,
  errorMessage: '',
}

function reducer(state: ProblemPageState, action: ProblemPageAction): ProblemPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, problems: action.problems, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export function useProblemPageModel() {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void listProblems()
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problems: response.items })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load problems.' })
      })

    return () => {
      cancelled = true
    }
  }, [])

  return state
}
