import { useCallback, useState } from 'react'

import { AuthClientError, deleteUser } from '@/features/auth/api/auth-client'
import type { Username } from '@/features/auth/domain/auth'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'

type DeleteUserResult =
  | { kind: 'deleted'; message: string }
  | { kind: 'forbidden' }
  | { kind: 'failed'; message: string }

export function useUserDeleteMutation() {
  const [deletingUsername, setDeletingUsername] = useState<Username | null>(null)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const deleteTargetUser = useCallback(
    async (targetUsername: Username): Promise<DeleteUserResult> => {
      setDeletingUsername(targetUsername)
      setNavigationIntent(null)

      try {
        const response = await deleteUser(targetUsername)
        setDeletingUsername(null)
        return { kind: 'deleted', message: response.message }
      } catch (error) {
        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setDeletingUsername(null)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        const message = error instanceof AuthClientError ? error.message : 'Unable to delete user.'
        setDeletingUsername(null)
        return { kind: 'failed', message }
      }
    },
    [],
  )

  return {
    deletingUsername,
    navigationIntent,
    deleteTargetUser,
  }
}
