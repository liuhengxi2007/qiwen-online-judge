import { useEffect, useState } from 'react'

import { AuthClientError, listRegisteredJudgers } from '@/features/auth/api/auth-client'
import { UserClientError, listUsers } from '@/features/user/api/user-client'
import type { AuthUserListItem } from '@/features/user/domain/user'
import type { RegisteredJudgerListItem } from '@/features/judger/model/RegisteredJudgerListItem'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { translateMessage } from '@/shared/i18n/messages'

export function useSiteManageQuery(siteManagerEnabled: boolean) {
  const [queryState, setQueryState] = useState<{
    enabled: boolean | null
    users: AuthUserListItem[]
    judgers: RegisteredJudgerListItem[]
    userListError: string
    judgerListError: string
    navigationIntent: NavigationIntent | null
    usersLoaded: boolean
    judgersLoaded: boolean
  }>({
    enabled: null,
    users: [],
    judgers: [],
    userListError: '',
    judgerListError: '',
    navigationIntent: null,
    usersLoaded: false,
    judgersLoaded: false,
  })

  useEffect(() => {
    if (!siteManagerEnabled) {
      return
    }

    let isCancelled = false

    void listUsers()
      .then((loadedUsers) => {
        if (isCancelled) {
          return
        }

        setQueryState((currentState) => ({
          ...currentState,
          enabled: siteManagerEnabled,
          users: loadedUsers,
          userListError: '',
          usersLoaded: true,
        }))
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return
        }

        if (error instanceof UserClientError && error.kind === 'forbidden') {
          setQueryState((currentState) => ({
            ...currentState,
            enabled: siteManagerEnabled,
            users: [],
            userListError: '',
            navigationIntent: toSiteManageDeniedRedirect(),
            usersLoaded: true,
          }))
          return
        }

        setQueryState((currentState) => ({
          ...currentState,
          enabled: siteManagerEnabled,
          users: [],
          userListError: translateMessage('siteManage.usersLoadFailed'),
          usersLoaded: true,
        }))
      })

    void listRegisteredJudgers()
      .then((loadedJudgers) => {
        if (isCancelled) {
          return
        }

        setQueryState((currentState) => ({
          ...currentState,
          enabled: siteManagerEnabled,
          judgers: loadedJudgers,
          judgerListError: '',
          judgersLoaded: true,
        }))
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return
        }

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setQueryState((currentState) => ({
            ...currentState,
            enabled: siteManagerEnabled,
            judgers: [],
            judgerListError: '',
            navigationIntent: toSiteManageDeniedRedirect(),
            judgersLoaded: true,
          }))
          return
        }

        setQueryState((currentState) => ({
          ...currentState,
          enabled: siteManagerEnabled,
          judgers: [],
          judgerListError: translateMessage('siteManage.judgersLoadFailed'),
          judgersLoaded: true,
        }))
      })

    return () => {
      isCancelled = true
    }
  }, [siteManagerEnabled])

  return {
    users: siteManagerEnabled && queryState.enabled === siteManagerEnabled ? queryState.users : [],
    judgers: siteManagerEnabled && queryState.enabled === siteManagerEnabled ? queryState.judgers : [],
    isLoadingUsers: siteManagerEnabled && (!queryState.usersLoaded || queryState.enabled !== siteManagerEnabled),
    isLoadingJudgers: siteManagerEnabled && (!queryState.judgersLoaded || queryState.enabled !== siteManagerEnabled),
    userListError: siteManagerEnabled && queryState.enabled === siteManagerEnabled ? queryState.userListError : '',
    judgerListError: siteManagerEnabled && queryState.enabled === siteManagerEnabled ? queryState.judgerListError : '',
    navigationIntent:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled ? queryState.navigationIntent : null,
    replaceUser(updatedUser: AuthUserListItem) {
      setQueryState((currentState) => ({
        ...currentState,
        users: currentState.users.map((currentUser) =>
          currentUser.username === updatedUser.username ? updatedUser : currentUser,
        ),
      }))
    },
    removeUser(targetUsername: AuthUserListItem['username']) {
      setQueryState((currentState) => ({
        ...currentState,
        users: currentState.users.filter((currentUser) => currentUser.username !== targetUsername),
      }))
    },
  }
}
