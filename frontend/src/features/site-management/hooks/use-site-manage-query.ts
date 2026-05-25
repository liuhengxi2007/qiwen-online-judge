import { useEffect, useState } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { listRegisteredJudgers } from '@/features/judger/http/api/ListRegisteredJudgers'
import { listUsers } from '@/features/user/http/api/ListUsers'
import type { AuthUserListItem } from '@/features/user/http/response/AuthUserListItem'
import type { UserListRequest } from '@/features/user/http/request/UserListRequest'
import type { RegisteredJudgerListItem } from '@/features/judger/http/response/RegisteredJudgerListItem'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toSiteManageDeniedRedirect } from '@/features/auth/lib/route-policy'
import { translateMessage } from '@/shared/i18n/messages'

export function useSiteManageQuery(siteManagerEnabled: boolean, userListRequest: UserListRequest) {
  const requestKey = JSON.stringify(userListRequest)
  const fallbackUserPage = userListRequest.pageRequest.page
  const fallbackUserPageSize = userListRequest.pageRequest.pageSize
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
    userPage: userListRequest.pageRequest.page,
    userPageSize: userListRequest.pageRequest.pageSize,
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

    const activeUserListRequest = JSON.parse(requestKey) as UserListRequest

    void listUsers(activeUserListRequest)
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

        if (error instanceof HttpClientError && error.kind === 'forbidden') {
          setQueryState((currentState) => ({
            ...currentState,
            enabled: siteManagerEnabled,
            requestKey,
            users: [],
            userPage: activeUserListRequest.pageRequest.page,
            userPageSize: activeUserListRequest.pageRequest.pageSize,
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
          userPage: activeUserListRequest.pageRequest.page,
          userPageSize: activeUserListRequest.pageRequest.pageSize,
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

        if (error instanceof HttpClientError && error.kind === 'forbidden') {
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
        : fallbackUserPage,
    userPageSize:
      siteManagerEnabled && queryState.enabled === siteManagerEnabled && queryState.requestKey === requestKey
        ? queryState.userPageSize
        : fallbackUserPageSize,
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
