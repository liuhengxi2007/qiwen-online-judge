import { create } from 'zustand'

import type { AuthUserListItem } from '@/features/auth/domain/auth'
import { AuthClientError, listUsers } from '@/features/auth/api/auth-client'

type UserDirectoryLoadResult =
  | { kind: 'loaded' }
  | { kind: 'forbidden' }
  | { kind: 'failed' }

type UserDirectoryStore = {
  users: AuthUserListItem[]
  isLoadingUsers: boolean
  userListError: string
  loadUsers: () => Promise<UserDirectoryLoadResult>
  replaceUser: (updatedUser: AuthUserListItem) => void
  removeUser: (username: AuthUserListItem['username']) => void
  reset: () => void
}

export const useUserDirectoryStore = create<UserDirectoryStore>()((set) => ({
  users: [],
  isLoadingUsers: false,
  userListError: '',
  loadUsers: async () => {
    set({
      users: [],
      isLoadingUsers: true,
      userListError: '',
    })

    try {
      const users = await listUsers()
      set({
        users,
        isLoadingUsers: false,
        userListError: '',
      })
      return { kind: 'loaded' }
    } catch (error) {
      if (error instanceof AuthClientError && error.kind === 'forbidden') {
        set({
          users: [],
          isLoadingUsers: false,
          userListError: '',
        })
        return { kind: 'forbidden' }
      }

      set({
        users: [],
        isLoadingUsers: false,
        userListError: 'Unable to load the user list.',
      })
      return { kind: 'failed' }
    }
  },
  replaceUser: (updatedUser) =>
    set((state) => ({
      users: state.users.map((currentUser) =>
        currentUser.username === updatedUser.username ? updatedUser : currentUser,
      ),
    })),
  removeUser: (username) =>
    set((state) => ({
      users: state.users.filter((currentUser) => currentUser.username !== username),
    })),
  reset: () =>
    set({
      users: [],
      isLoadingUsers: false,
      userListError: '',
    }),
}))
