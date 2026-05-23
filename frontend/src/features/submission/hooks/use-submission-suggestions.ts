import { useEffect, useState } from 'react'

import { listProblemSuggestions } from '@/features/problem/http/api/problem-client'
import type { ProblemSuggestion } from '@/features/problem/http/response/ProblemSuggestion'
import { listUserSuggestions } from '@/features/user/http/api/user-client'
import type { UserIdentity } from '@/features/user/model/UserIdentity'

type UseSubmissionSuggestionsArgs = {
  usernameFilterInput: string
  problemFilterInput: string
  showUserSuggestionPanel: boolean
  showProblemSuggestionPanel: boolean
}

export function useSubmissionSuggestions({
  usernameFilterInput,
  problemFilterInput,
  showUserSuggestionPanel,
  showProblemSuggestionPanel,
}: UseSubmissionSuggestionsArgs) {
  const [isLoadingUserSuggestions, setIsLoadingUserSuggestions] = useState(false)
  const [isLoadingProblemSuggestions, setIsLoadingProblemSuggestions] = useState(false)
  const [userSuggestions, setUserSuggestions] = useState<UserIdentity[]>([])
  const [problemSuggestions, setProblemSuggestions] = useState<ProblemSuggestion[]>([])

  useEffect(() => {
    if (!showUserSuggestionPanel) {
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      setIsLoadingUserSuggestions(true)
      void listUserSuggestions(usernameFilterInput.trim())
        .then((suggestions) => {
          if (!cancelled) {
            setUserSuggestions(suggestions)
            setIsLoadingUserSuggestions(false)
          }
        })
        .catch(() => {
          if (!cancelled) {
            setUserSuggestions([])
            setIsLoadingUserSuggestions(false)
          }
        })
    }, 150)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [showUserSuggestionPanel, usernameFilterInput])

  useEffect(() => {
    if (!showProblemSuggestionPanel) {
      return
    }

    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      setIsLoadingProblemSuggestions(true)
      void listProblemSuggestions(problemFilterInput.trim())
        .then((suggestions) => {
          if (!cancelled) {
            setProblemSuggestions(suggestions)
            setIsLoadingProblemSuggestions(false)
          }
        })
        .catch(() => {
          if (!cancelled) {
            setProblemSuggestions([])
            setIsLoadingProblemSuggestions(false)
          }
        })
    }, 150)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [problemFilterInput, showProblemSuggestionPanel])

  return {
    isLoadingUserSuggestions: showUserSuggestionPanel && isLoadingUserSuggestions,
    isLoadingProblemSuggestions: showProblemSuggestionPanel && isLoadingProblemSuggestions,
    userSuggestions: showUserSuggestionPanel ? userSuggestions : [],
    problemSuggestions: showProblemSuggestionPanel ? problemSuggestions : [],
  }
}
