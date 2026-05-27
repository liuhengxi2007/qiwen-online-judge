import { useCallback, useState } from 'react'

import { toAuthSession } from '@/pages/objects/auth/auth-session'
import type { LoginRequest } from '@/objects/auth/request/LoginRequest'
import type { LoginResponse } from '@/objects/auth/response/LoginResponse'
import { login } from '@/apis/auth/Login'
import type { NavigationIntent } from '@/pages/objects/navigation-intent'
import { useAuthStore } from '@/pages/objects/auth/use-auth-store'

type LoginMutationResult =
  | { kind: 'succeeded'; data: LoginResponse }
  | { kind: 'failed'; message: string }

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
        const data = await login(request)
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
