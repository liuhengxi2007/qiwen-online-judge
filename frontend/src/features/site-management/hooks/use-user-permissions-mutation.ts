import { useCallback, useState } from 'react'

import type { Username } from '@/features/user/model/Username'
import { HttpClientError } from '@/shared/api/http-client'
import { updateAccountPermissions } from '@/features/auth/http/api/UpdateAccountPermissions'
import type { AuthUserListItem } from '@/features/user/model/response/AuthUserListItem'
import type { UpdateUserPermissionsRequest } from '@/features/auth/model/request/UpdateUserPermissionsRequest'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { translateMessage } from '@/shared/i18n/messages'

type SavePermissionsResult =
  | { kind: 'updated'; user: AuthUserListItem }
  | { kind: 'forbidden' }
  | { kind: 'failed'; message: string }

export function useUserPermissionsMutation() {
  const [updatingUsername, setUpdatingUsername] = useState<Username | null>(null)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const savePermissions = useCallback(
    async (targetUsername: Username, nextPermissions: UpdateUserPermissionsRequest): Promise<SavePermissionsResult> => {
      setUpdatingUsername(targetUsername)
      setNavigationIntent(null)

      try {
        const updatedUser = await updateAccountPermissions(targetUsername, nextPermissions)
        setUpdatingUsername(null)
        return { kind: 'updated', user: updatedUser }
      } catch (error) {
        if (error instanceof HttpClientError && error.kind === 'forbidden') {
          setUpdatingUsername(null)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        const message = translateMessage('siteManage.message.updatePermissionsFailed')
        setUpdatingUsername(null)
        return { kind: 'failed', message }
      }
    },
    [],
  )

  return {
    updatingUsername,
    navigationIntent,
    savePermissions,
  }
}
