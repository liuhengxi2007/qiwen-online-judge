import { useEffect, useState } from 'react'

import { AuthClientError, listRegisteredJudgers, listUsers } from '@/features/auth/api/auth-client'
import type { AuthUserListItem } from '@/features/auth/domain/auth'
import type { RegisteredJudgerListItem } from '@/features/judger/model/RegisteredJudgerListItem'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'

export function useSiteManageQuery(siteManagerEnabled: boolean) {
  const [users, setUsers] = useState<AuthUserListItem[]>([])
  const [judgers, setJudgers] = useState<RegisteredJudgerListItem[]>([])
  const [isLoadingUsers, setIsLoadingUsers] = useState(false)
  const [isLoadingJudgers, setIsLoadingJudgers] = useState(false)
  const [userListError, setUserListError] = useState('')
  const [judgerListError, setJudgerListError] = useState('')
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  useEffect(() => {
    if (!siteManagerEnabled) {
      setUsers([])
      setJudgers([])
      setIsLoadingUsers(false)
      setIsLoadingJudgers(false)
      setUserListError('')
      setJudgerListError('')
      setNavigationIntent(null)
      return
    }

    let isCancelled = false
    setUsers([])
    setJudgers([])
    setIsLoadingUsers(true)
    setIsLoadingJudgers(true)
    setUserListError('')
    setJudgerListError('')
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

    void listRegisteredJudgers()
      .then((loadedJudgers) => {
        if (isCancelled) {
          return
        }

        setJudgers(loadedJudgers)
        setIsLoadingJudgers(false)
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return
        }

        setJudgers([])
        setIsLoadingJudgers(false)

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setJudgerListError('')
          setNavigationIntent(toSiteManageDeniedRedirect())
          return
        }

        setJudgerListError('Unable to load the registered judgers.')
      })

    return () => {
      isCancelled = true
    }
  }, [siteManagerEnabled])

  return {
    users,
    judgers,
    isLoadingUsers,
    isLoadingJudgers,
    userListError,
    judgerListError,
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
