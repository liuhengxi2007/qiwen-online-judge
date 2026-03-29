import { useCallback, useState } from 'react'

import { toAuthSession, type RegisterRequest, type RegisterResponse } from '@/features/auth/domain/auth'
import { register } from '@/features/auth/api/auth-client'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { useAuthStore } from '@/features/auth/stores/use-auth-store'

type RegisterMutationResult =
  | { kind: 'succeeded'; data: RegisterResponse }
  | { kind: 'failed'; message: string }

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
        const data = await register(request)
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
