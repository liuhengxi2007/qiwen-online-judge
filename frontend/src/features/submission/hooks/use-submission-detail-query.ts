import { useEffect, useReducer } from 'react'

import { getSubmission } from '@/features/submission/api/submission-client'
import { isTerminalSubmissionStatus, type SubmissionDetail, type SubmissionId } from '@/features/submission/domain/submission'

type SubmissionDetailQueryState = {
  submission: SubmissionDetail | null
  isLoading: boolean
  errorMessage: string
}

type SubmissionDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; submission: SubmissionDetail }
  | { type: 'load_failed'; message: string }

const initialState: SubmissionDetailQueryState = {
  submission: null,
  isLoading: true,
  errorMessage: '',
}

function reducer(state: SubmissionDetailQueryState, action: SubmissionDetailQueryAction): SubmissionDetailQueryState {
  switch (action.type) {
    case 'load_started':
      return { ...state, isLoading: true, errorMessage: '' }
    case 'load_succeeded':
      return { submission: action.submission, isLoading: false, errorMessage: '' }
    case 'load_failed':
      return { submission: null, isLoading: false, errorMessage: action.message }
  }
}

export function useSubmissionDetailQuery(submissionId: SubmissionId) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null
    dispatch({ type: 'load_started' })

    const load = () => {
      void getSubmission(submissionId)
        .then((submission) => {
          if (cancelled) {
            return
          }

          dispatch({ type: 'load_succeeded', submission })
          if (isTerminalSubmissionStatus(submission.status) && intervalId !== null) {
            window.clearInterval(intervalId)
            intervalId = null
          }
        })
        .catch(() => {
          if (cancelled) {
            return
          }

          dispatch({ type: 'load_failed', message: 'Unable to load submission details.' })
        })
    }

    load()
    intervalId = window.setInterval(load, 2000)

    return () => {
      cancelled = true
      if (intervalId !== null) {
        window.clearInterval(intervalId)
      }
    }
  }, [submissionId])

  return state
}
