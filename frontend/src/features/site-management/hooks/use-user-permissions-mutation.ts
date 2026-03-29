import { useCallback, useState } from 'react'

import { type AuthUserListItem, type UpdateUserPermissionsRequest } from '@/features/auth/domain/auth'
import { AuthClientError, updateUserPermissions } from '@/features/auth/api/auth-client'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { useUserDirectoryStore } from '@/features/site-management/stores/use-user-directory-store'

type SavePermissionsResult =
  | { kind: 'updated'; user: AuthUserListItem }
  | { kind: 'forbidden' }
  | { kind: 'failed'; message: string }

export function useUserPermissionsMutation() {
  const replaceUser = useUserDirectoryStore((state) => state.replaceUser)
  const [updatingUsername, setUpdatingUsername] = useState<string | null>(null)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const savePermissions = useCallback(
    async (targetUsername: string, nextPermissions: UpdateUserPermissionsRequest): Promise<SavePermissionsResult> => {
      setUpdatingUsername(targetUsername)
      setNavigationIntent(null)

      try {
        const updatedUser = await updateUserPermissions(targetUsername, nextPermissions)
        replaceUser(updatedUser)
        setUpdatingUsername(null)
        return { kind: 'updated', user: updatedUser }
      } catch (error) {
        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setUpdatingUsername(null)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        const message = 'Unable to update user permissions.'
        setUpdatingUsername(null)
        return { kind: 'failed', message }
      }
    },
    [replaceUser],
  )

  return {
    updatingUsername,
    navigationIntent,
    savePermissions,
  }
}
