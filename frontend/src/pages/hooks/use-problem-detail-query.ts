import { useEffect, useReducer } from 'react'

import { getProblem } from '@/apis/problem/GetProblem'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import {
  initialProblemDetailQueryState,
  reduceProblemDetailQueryState,
} from '@/pages/objects/problem/problem-query-state'
import { HttpClientError } from '@/system/api/http-client'

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
  }, [problemSlug])

  function replaceProblem(problem: ProblemDetail) {
    dispatch({ type: 'replace', problem })
  }

  return {
    ...state,
    replaceProblem,
  }
}
