import { useEffect, useState } from 'react'

import { AuthClientError, listUsers } from '@/features/auth/api/auth-client'
import type { AuthUserListItem } from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'

export function useSiteManageQuery(siteManagerEnabled: boolean) {
  const [users, setUsers] = useState<AuthUserListItem[]>([])
  const [isLoadingUsers, setIsLoadingUsers] = useState(false)
  const [userListError, setUserListError] = useState('')
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  useEffect(() => {
    if (!siteManagerEnabled) {
      setUsers([])
      setIsLoadingUsers(false)
      setUserListError('')
      setNavigationIntent(null)
      return
    }

    let isCancelled = false
    setUsers([])
    setIsLoadingUsers(true)
    setUserListError('')
    setNavigationIntent(null)

    void listUsers()
      .then((loadedUsers) => {
        if (isCancelled) {
          return
        }

        setUsers(loadedUsers)
        setIsLoadingUsers(false)
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return
        }

        setUsers([])
        setIsLoadingUsers(false)

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setUserListError('')
          setNavigationIntent(toSiteManageDeniedRedirect())
          return
        }

        setUserListError('Unable to load the user list.')
      })

    return () => {
      isCancelled = true
    }
  }, [siteManagerEnabled])

  return {
    users,
    isLoadingUsers,
    userListError,
    navigationIntent,
    replaceUser(updatedUser: AuthUserListItem) {
      setUsers((currentUsers) =>
        currentUsers.map((currentUser) => (currentUser.username === updatedUser.username ? updatedUser : currentUser)),
      )
    },
    removeUser(targetUsername: AuthUserListItem['username']) {
      setUsers((currentUsers) => currentUsers.filter((currentUser) => currentUser.username !== targetUsername))
    },
  }
}
