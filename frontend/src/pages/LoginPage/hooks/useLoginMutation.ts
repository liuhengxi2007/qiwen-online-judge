import { useCallback, useState } from 'react'

import { toAuthSession } from '@/pages/stores/auth/AuthSession'
import type { LoginRequest } from '@/objects/auth/request/LoginRequest'
import type { LoginResponse } from '@/objects/auth/response/LoginResponse'
import { Login } from '@/apis/auth/Login'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { sendAPI } from '@/system/api/api-message'

/**
 * 登录请求结果，成功时返回会话，失败时返回错误消息。
 */
type LoginMutationResult =
  | { kind: 'succeeded'; data: LoginResponse }
  | { kind: 'failed'; message: string }

/**
 * 登录请求 hook；调用登录 API 并将响应规范化为前端会话。
 */
export function useLoginMutation() {
  const setSession = useAuthStore((state) => state.setSession)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const submitLogin = useCallback(
    async (request: LoginRequest): Promise<LoginMutationResult> => {
      setIsSubmitting(true)
      setErrorMessage('')
      setNavigationIntent(null)

      try {
        const data = await sendAPI(new Login(request))
        setSession(toAuthSession(data))
        setIsSubmitting(false)
        setNavigationIntent({ to: '/' })
        return { kind: 'succeeded', data }
      } catch (error) {
        const message =
          error instanceof Error
            ? error.message
            : 'Unable to reach the server. Please start the backend service.'
        setIsSubmitting(false)
        setErrorMessage(message)
        return { kind: 'failed', message }
      }
    },
    [setSession],
  )

  return {
    isSubmitting,
    errorMessage,
    navigationIntent,
    submitLogin,
  }
}
