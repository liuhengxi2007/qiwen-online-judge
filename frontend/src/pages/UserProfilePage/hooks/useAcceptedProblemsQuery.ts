import { useEffect, useRef, useState } from 'react'

import { ListUserAcceptedProblems } from '@/apis/user/ListUserAcceptedProblems'
import type { PageResponse } from '@/objects/shared/PageResponse'
import type { UserAcceptedProblem } from '@/objects/user/UserAcceptedProblem'
import type { Username } from '@/objects/user/Username'
import { sendAPI } from '@/system/api/api-message'
import { translateMessage } from '@/system/i18n/messages'

type UseAcceptedProblemsQueryArgs = {
  enabled: boolean
  page: number
  targetUsername: Username
}

type AcceptedProblemsState = {
  username: Username | null
  page: number
  response: PageResponse<UserAcceptedProblem> | null
  errorMessage: string
  isLoading: boolean
}

/** 按需加载用户已通过题目分页，使用 request id 丢弃过期响应。 */
export function useAcceptedProblemsQuery({ enabled, page, targetUsername }: UseAcceptedProblemsQueryArgs) {
  const [state, setState] = useState<AcceptedProblemsState>({
    username: null,
    page: 1,
    response: null,
    errorMessage: '',
    isLoading: false,
  })
  const requestIdRef = useRef(0)

  useEffect(() => {
    if (!enabled) {
      return
    }

    let isCancelled = false
    requestIdRef.current += 1
    const nextRequestId = requestIdRef.current

    setState({
      username: targetUsername,
      page,
      response: null,
      errorMessage: '',
      isLoading: true,
    })

    void sendAPI(new ListUserAcceptedProblems(targetUsername, page))
      .then((response) => {
        if (isCancelled || requestIdRef.current !== nextRequestId) {
          return
        }

        setState({
          username: targetUsername,
          page,
          response,
          errorMessage: '',
          isLoading: false,
        })
      })
      .catch(() => {
        if (isCancelled || requestIdRef.current !== nextRequestId) {
          return
        }

        setState({
          username: targetUsername,
          page,
          response: null,
          errorMessage: translateMessage('userProfile.acceptedProblemsLoadFailed'),
          isLoading: false,
        })
      })

    return () => {
      isCancelled = true
    }
  }, [enabled, page, targetUsername])

  const isCurrentResponse = enabled && state.username === targetUsername && state.page === page

  return {
    response: isCurrentResponse ? state.response : null,
    errorMessage: isCurrentResponse ? state.errorMessage : '',
    isLoading: enabled && (!isCurrentResponse || state.isLoading),
  }
}
