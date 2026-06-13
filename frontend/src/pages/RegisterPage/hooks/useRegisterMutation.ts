import { useCallback, useState } from 'react'

import { toAuthSession } from '@/pages/stores/auth/AuthSession'
import type { RegisterRequest } from '@/objects/auth/request/RegisterRequest'
import type { RegisterResponse } from '@/objects/auth/response/RegisterResponse'
import { Register } from '@/apis/auth/Register'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import { useAuthStore } from '@/pages/stores/auth/UseAuthStore'
import { sendAPI } from '@/system/api/api-message'

/**
 * 注册请求结果，成功时返回登录会话，失败时返回错误消息。
 */
type RegisterMutationResult =
  | { kind: 'succeeded'; data: RegisterResponse }
  | { kind: 'failed'; message: string }

/**
 * 注册请求 hook；提交注册 API 并把响应规范化为会话对象。
 */
export function useRegisterMutation() {
  const setSession = useAuthStore((state) => state.setSession)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const submitRegister = useCallback(
    async (request: RegisterRequest): Promise<RegisterMutationResult> => {
      setIsSubmitting(true)
      setErrorMessage('')
      setNavigationIntent(null)

      try {
        const data = await sendAPI(new Register(request))
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
    submitRegister,
  }
}
