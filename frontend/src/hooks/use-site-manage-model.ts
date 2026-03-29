import { useCallback, useEffect, useReducer } from 'react'
import { useNavigate } from 'react-router-dom'

import { usernameValue, type AuthUserListItem, type UpdateUserPermissionsRequest } from '@/domain/auth'
import { AuthClientError, listUsers, updateUserPermissions } from '@/lib/auth-client'

type SiteManageState = {
  users: AuthUserListItem[]
  userListError: string
  statusMessage: string
  isLoadingUsers: boolean
  updatingUsername: string | null
}

type SiteManageAction =
  | { type: 'load_started' }
  | { type: 'load_succeeded'; users: AuthUserListItem[] }
  | { type: 'load_failed'; message: string }
  | { type: 'update_started'; username: string }
  | { type: 'update_succeeded'; user: AuthUserListItem }
  | { type: 'update_failed'; message: string }

const initialState: SiteManageState = {
  users: [],
  userListError: '',
  statusMessage: '',
  isLoadingUsers: false,
  updatingUsername: null,
}

function replaceUser(users: AuthUserListItem[], updatedUser: AuthUserListItem): AuthUserListItem[] {
  return users.map((currentUser) =>
    usernameValue(currentUser.username) === usernameValue(updatedUser.username) ? updatedUser : currentUser,
  )
}

function siteManageReducer(state: SiteManageState, action: SiteManageAction): SiteManageState {
  switch (action.type) {
    case 'load_started':
      return {
        ...state,
        isLoadingUsers: true,
        userListError: '',
        statusMessage: '',
      }
    case 'load_succeeded':
      return {
        ...state,
        users: action.users,
        isLoadingUsers: false,
        userListError: '',
      }
    case 'load_failed':
      return {
        ...state,
        isLoadingUsers: false,
        userListError: action.message,
      }
    case 'update_started':
      return {
        ...state,
        updatingUsername: action.username,
        userListError: '',
        statusMessage: '',
      }
    case 'update_succeeded':
      return {
        ...state,
        users: replaceUser(state.users, action.user),
        updatingUsername: null,
        statusMessage: `Permissions updated for ${usernameValue(action.user.username)}.`,
      }
    case 'update_failed':
      return {
        ...state,
        updatingUsername: null,
        userListError: action.message,
      }
  }
}

export function useSiteManageModel(siteManagerEnabled: boolean) {
  const navigate = useNavigate()
  const [state, dispatch] = useReducer(siteManageReducer, initialState)

  useEffect(() => {
    if (!siteManagerEnabled) {
      navigate('/')
      return
    }

    let isCancelled = false

    const load = async () => {
      dispatch({ type: 'load_started' })

      try {
        const users = await listUsers()

        if (!isCancelled) {
          dispatch({ type: 'load_succeeded', users })
        }
      } catch (error) {
        if (isCancelled) {
          return
        }

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          navigate('/?notice=site-manage-denied', { replace: true })
          return
        }

        dispatch({ type: 'load_failed', message: 'Unable to load the user list.' })
      }
    }

    void load()

    return () => {
      isCancelled = true
    }
  }, [navigate, siteManagerEnabled])

  const savePermissions = useCallback(
    async (listedUser: AuthUserListItem, nextPermissions: UpdateUserPermissionsRequest) => {
      const targetUsername = usernameValue(listedUser.username)
      dispatch({ type: 'update_started', username: targetUsername })

      try {
        const updatedUser = await updateUserPermissions(targetUsername, nextPermissions)
        dispatch({ type: 'update_succeeded', user: updatedUser })
      } catch (error) {
        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          navigate('/?notice=site-manage-denied', { replace: true })
          return
        }

        dispatch({ type: 'update_failed', message: 'Unable to update user permissions.' })
      }
    },
    [navigate],
  )

  return {
    ...state,
    savePermissions,
  }
}
