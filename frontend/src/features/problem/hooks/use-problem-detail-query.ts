import { useEffect, useReducer } from 'react'

import { getProblem } from '@/features/problem/api/problem-client'
import type { ProblemDetail, ProblemSlug } from '@/features/problem/domain/problem'
import {
  initialProblemDetailQueryState,
  reduceProblemDetailQueryState,
} from '@/features/problem/domain/problem-query-state'

export function useProblemDetailQuery(problemSlug: ProblemSlug) {
  const [state, dispatch] = useReducer(reduceProblemDetailQueryState, initialProblemDetailQueryState)

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
