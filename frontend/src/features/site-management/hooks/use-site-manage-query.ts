import { useEffect, useState } from 'react'

import { AuthClientError, listRegisteredJudgers } from '@/features/auth/api/auth-client'
import { UserClientError, listUsers } from '@/features/user/api/user-client'
import type { AuthUserListItem, UserListRequest } from '@/features/user/domain/user'
import type { RegisteredJudgerListItem } from '@/features/judger/model/RegisteredJudgerListItem'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { translateMessage } from '@/shared/i18n/messages'

export function useSiteManageQuery(siteManagerEnabled: boolean, userListRequest: UserListRequest) {
  const requestKey = JSON.stringify(userListRequest)
  const [queryState, setQueryState] = useState<{
    enabled: boolean | null
    requestKey: string
    users: AuthUserListItem[]
    userPage: number
    userPageSize: number
    totalUsers: number
    judgers: RegisteredJudgerListItem[]
    userListError: string
    judgerListError: string
    navigationIntent: NavigationIntent | null
    usersLoaded: boolean
    judgersLoaded: boolean
  }>({
    enabled: null,
    requestKey: '',
    users: [],
    userPage: userListRequest.page,
    userPageSize: userListRequest.pageSize,
    totalUsers: 0,
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

    void listUsers(userListRequest)
      .then((loadedUsers) => {
        if (isCancelled) {
          return
        }

        setQueryState((currentState) => ({
          ...currentState,
          enabled: siteManagerEnabled,
          requestKey,
          users: loadedUsers.items,
          userPage: loadedUsers.page,
          userPageSize: loadedUsers.pageSize,
          totalUsers: loadedUsers.totalItems,
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
            requestKey,
            users: [],
            userPage: userListRequest.page,
            userPageSize: userListRequest.pageSize,
            totalUsers: 0,
            userListError: '',
            navigationIntent: toSiteManageDeniedRedirect(),
            usersLoaded: true,
          }))
          return
        }

        setQueryState((currentState) => ({
          ...currentState,
          enabled: siteManagerEnabled,
          requestKey,
          users: [],
          userPage: userListRequest.page,
          userPageSize: userListRequest.pageSize,
          totalUsers: 0,
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
  }, [requestKey, siteManagerEnabled])

  return {
    users:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled && queryState.requestKey === requestKey
        ? queryState.users
        : [],
    userPage:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled && queryState.requestKey === requestKey
        ? queryState.userPage
        : userListRequest.page,
    userPageSize:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled && queryState.requestKey === requestKey
        ? queryState.userPageSize
        : userListRequest.pageSize,
    totalUsers:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled && queryState.requestKey === requestKey
        ? queryState.totalUsers
        : 0,
    judgers: siteManagerEnabled && queryState.enabled === siteManagerEnabled ? queryState.judgers : [],
    isLoadingUsers:
      siteManagerEnabled &&
      (!queryState.usersLoaded || queryState.enabled !== siteManagerEnabled || queryState.requestKey !== requestKey),
    isLoadingJudgers: siteManagerEnabled && (!queryState.judgersLoaded || queryState.enabled !== siteManagerEnabled),
    userListError:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled && queryState.requestKey === requestKey
        ? queryState.userListError
        : '',
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
        totalUsers: Math.max(0, currentState.totalUsers - 1),
      }))
    },
  }
}
