import { useEffect, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import { listSubmissions } from '@/features/submission/api/submission-client'
import { isTerminalSubmissionStatus, type SubmissionSummary } from '@/features/submission/domain/submission'

export function useSubmissionListQuery(submitterUsername: Username | null) {
  const [submissions, setSubmissions] = useState<SubmissionSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null
    setSubmissions([])
    setIsLoading(true)
    setErrorMessage('')

    const load = () => {
      void listSubmissions(submitterUsername)
        .then((loadedSubmissions) => {
          if (cancelled) {
            return
          }

          setSubmissions(loadedSubmissions)
          setIsLoading(false)
          setErrorMessage('')
          if (!loadedSubmissions.some((submission) => !isTerminalSubmissionStatus(submission.status)) && intervalId !== null) {
            window.clearInterval(intervalId)
            intervalId = null
          }
        })
        .catch(() => {
          if (cancelled) {
            return
          }

          setSubmissions([])
          setIsLoading(false)
          setErrorMessage('Unable to load submissions.')
        })
    }

    load()
    intervalId = window.setInterval(load, 3000)

    return () => {
      cancelled = true
      if (intervalId !== null) {
        window.clearInterval(intervalId)
      }
    }
  }, [submitterUsername])

  return {
    submissions,
    isLoading,
    errorMessage,
  }
}
