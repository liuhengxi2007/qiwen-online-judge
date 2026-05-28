import { useEffect, useReducer } from 'react'

import { GetSubmission } from '@/apis/submission/GetSubmission'
import { isTerminalSubmissionStatus } from '@/objects/submission/SubmissionStatus'
import type { SubmissionDetail } from '@/objects/submission/response/SubmissionDetail'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'

type SubmissionDetailQueryState = {
  submission: SubmissionDetail | null
  isLoading: boolean
  errorMessage: string
}

type SubmissionDetailQueryAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; submission: SubmissionDetail }
  | { type: 'load_failed'; message: string }
  | { type: 'replace_submission'; submission: SubmissionDetail }

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
    case 'replace_submission':
      return { submission: action.submission, isLoading: false, errorMessage: '' }
  }
}

export function useSubmissionDetailQuery(submissionId: SubmissionId) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null
    dispatch({ type: 'load_started' })

    const load = () => {
      void sendAPI(new GetSubmission(submissionId))
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
        .catch((error: unknown) => {
          if (cancelled) {
            return
          }

          dispatch({
            type: 'load_failed',
            message:
              error instanceof HttpClientError && (error.kind === 'not-found' || error.kind === 'forbidden')
                ? '404 Not Found.'
                : 'Unable to load submission details.',
          })
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

  return {
    ...state,
    replaceSubmission: (submission: SubmissionDetail) => dispatch({ type: 'replace_submission', submission }),
  }
}
