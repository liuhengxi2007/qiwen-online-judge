import { useCallback, useState } from 'react'

import type { Username } from '@/features/user/model/Username'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { HttpClientError } from '@/shared/api/http-client'
import { deleteUser } from '@/features/user/http/api/DeleteUser'
import { translateMessage } from '@/shared/i18n/messages'
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
        return { kind: 'deleted', message: response.message ?? translateMessage('common.success.generic') }
      } catch (error) {
        if (error instanceof HttpClientError && error.kind === 'forbidden') {
          setDeletingUsername(null)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        const message =
          error instanceof HttpClientError ? error.message : translateMessage('siteManage.message.deleteUserFailed')
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
