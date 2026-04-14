import { useEffect, useReducer } from 'react'

import { listProblems } from '@/features/problem/api/problem-client'
import {
  initialProblemPageState,
  reduceProblemPageState,
} from '@/features/problem/domain/problem-query-state'

export function useProblemPageModel() {
  const [state, dispatch] = useReducer(reduceProblemPageState, initialProblemPageState)

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
