import { useCallback, useState } from 'react'

import type { Username } from '@/objects/user/Username'
import { HttpClientError } from '@/system/api/http-client'
import { updateAccountPermissions } from '@/apis/auth/UpdateAccountPermissions'
import type { AuthUserListItem } from '@/objects/user/response/AuthUserListItem'
import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { NavigationIntent } from '@/pages/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/pages/routing/route-policy'
import { translateMessage } from '@/system/i18n/messages'

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
