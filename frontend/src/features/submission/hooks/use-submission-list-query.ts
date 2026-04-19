import { useEffect, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import { listSubmissions } from '@/features/submission/api/submission-client'
import { isTerminalSubmissionStatus, type SubmissionSummary } from '@/features/submission/domain/submission'

export function useSubmissionListQuery(submitterUsername: Username | null) {
  const [queryState, setQueryState] = useState<{
    username: Username | null
    submissions: SubmissionSummary[]
    errorMessage: string
  }>({
    username: null,
    submissions: [],
    errorMessage: '',
  })

  useEffect(() => {
    let cancelled = false
    let intervalId: number | null = null

    const load = () => {
      void listSubmissions(submitterUsername)
        .then((loadedSubmissions) => {
          if (cancelled) {
            return
          }

          setQueryState({
            username: submitterUsername,
            submissions: loadedSubmissions,
            errorMessage: '',
          })
          if (!loadedSubmissions.some((submission) => !isTerminalSubmissionStatus(submission.status)) && intervalId !== null) {
            window.clearInterval(intervalId)
            intervalId = null
          }
        })
        .catch(() => {
          if (cancelled) {
            return
          }

          setQueryState({
            username: submitterUsername,
            submissions: [],
            errorMessage: 'Unable to load submissions.',
          })
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
    submissions: queryState.username === submitterUsername ? queryState.submissions : [],
    isLoading: queryState.username !== submitterUsername,
    errorMessage: queryState.username === submitterUsername ? queryState.errorMessage : '',
  }
}
