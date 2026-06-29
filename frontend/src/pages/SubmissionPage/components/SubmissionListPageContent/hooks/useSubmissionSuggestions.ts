import { useEffect, useState } from 'react'

import { ListProblemSuggestions } from '@/apis/problem/ListProblemSuggestions'
import { parseProblemSearchQuery } from '@/objects/problem/request/ProblemSearchQuery'
import type { ProblemSuggestion } from '@/objects/problem/response/ProblemSuggestion'
import { parseUserSearchQuery } from '@/objects/user/request/UserSearchQuery'
import { ListUserSuggestions } from '@/apis/user/ListUserSuggestions'
import type { UserIdentity } from '@/objects/user/UserIdentity'
import { sendAPI } from '@/system/api/api-message'

/**
 * 提交筛选建议 hook 的输入，包含当前输入和两个建议面板的显示状态。
 */
type UseSubmissionSuggestionsArgs = {
  usernameFilterInput: string
  problemFilterInput: string
  showUserSuggestionPanel: boolean
  showProblemSuggestionPanel: boolean
}

/**
 * 为提交筛选框加载用户和题目建议；输入变化后延迟请求，面板关闭时返回空建议。
 */
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
      const parsedQuery = parseUserSearchQuery(usernameFilterInput)
      if (!parsedQuery.ok) {
        setUserSuggestions([])
        return
      }

      setIsLoadingUserSuggestions(true)
      void sendAPI(new ListUserSuggestions(parsedQuery.value))
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
      const parsedQuery = parseProblemSearchQuery(problemFilterInput)
      if (!parsedQuery.ok) {
        setProblemSuggestions([])
        return
      }

      setIsLoadingProblemSuggestions(true)
      void sendAPI(new ListProblemSuggestions(parsedQuery.value))
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
