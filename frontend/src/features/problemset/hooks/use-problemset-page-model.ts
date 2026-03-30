import { useEffect, useReducer } from 'react'

import { listProblemSets } from '@/features/problemset/api/problemset-client'
import type { ProblemSetSummary } from '@/features/problemset/domain/problemset'

type ProblemSetPageState = {
  problemSets: ProblemSetSummary[]
  isLoading: boolean
  errorMessage: string
}

type ProblemSetPageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; problemSets: ProblemSetSummary[] }
  | { type: 'load_failed'; message: string }

const initialState: ProblemSetPageState = {
  problemSets: [],
  isLoading: true,
  errorMessage: '',
}

function reducer(state: ProblemSetPageState, action: ProblemSetPageAction): ProblemSetPageState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { ...state, isLoading: false, problemSets: action.problemSets, errorMessage: '' }
    case 'load_failed':
      return { ...state, isLoading: false, errorMessage: action.message }
  }
}

export function useProblemSetPageModel() {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    dispatch({ type: 'load_started' })
    void listProblemSets()
      .then((response) => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_succeeded', problemSets: response.items })
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        dispatch({ type: 'load_failed', message: 'Unable to load problem sets.' })
      })

    return () => {
      cancelled = true
    }
  }, [])

  return state
}
