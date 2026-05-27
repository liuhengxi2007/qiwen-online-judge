import { useCallback, useState } from 'react'

import type { Username } from '@/objects/user/Username'
import { toSiteManageDeniedRedirect } from '@/pages/objects/auth/route-policy'
import { HttpClientError } from '@/system/api/http-client'
import { deleteAccount } from '@/apis/auth/DeleteAccount'
import { translateMessage } from '@/system/i18n/messages'
import type { NavigationIntent } from '@/pages/objects/navigation-intent'

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
        const response = await deleteAccount(targetUsername)
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
