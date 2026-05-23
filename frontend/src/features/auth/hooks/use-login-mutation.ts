import { useCallback, useState } from 'react'

import { toAuthSession } from '@/features/auth/lib/auth-session'
import type { LoginRequest } from '@/features/auth/http/request/LoginRequest'
import type { LoginResponse } from '@/features/auth/http/response/LoginResponse'
import { login } from '@/features/auth/http/api/auth-client'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

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
