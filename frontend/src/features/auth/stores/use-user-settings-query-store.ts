import { create } from 'zustand'

import { sameUsername, type SessionResponse, type Username } from '@/features/auth/domain/auth'
import { AuthClientError, getUserSettings } from '@/features/auth/api/auth-client'

type UserSettingsLoadResult =
  | { kind: 'loaded' }
  | { kind: 'forbidden' }
  | { kind: 'not-found' }
  | { kind: 'failed' }
  | { kind: 'stale' }

type UserSettingsQueryStore = {
  activeTargetUsername: Username | null
  editedUser: SessionResponse | null
  isLoadingSettings: boolean
  settingsLoadError: string
  requestId: number
  loadUserSettings: (username: Username) => Promise<UserSettingsLoadResult>
  setEditedUser: (username: Username, editedUser: SessionResponse) => void
  reset: () => void
}

export const useUserSettingsQueryStore = create<UserSettingsQueryStore>()((set, get) => ({
  activeTargetUsername: null,
  editedUser: null,
  isLoadingSettings: false,
  settingsLoadError: '',
  requestId: 0,
  loadUserSettings: async (username) => {
    const nextRequestId = get().requestId + 1

    set({
      activeTargetUsername: username,
      editedUser: null,
      isLoadingSettings: true,
      settingsLoadError: '',
      requestId: nextRequestId,
    })

    try {
      const editedUser = await getUserSettings(username)

      if (get().requestId != nextRequestId) {
        return { kind: 'stale' }
      }

      set({
        activeTargetUsername: username,
        editedUser,
        isLoadingSettings: false,
        settingsLoadError: '',
      })
      return { kind: 'loaded' }
    } catch (error) {
      if (get().requestId != nextRequestId) {
        return { kind: 'stale' }
      }

      if (error instanceof AuthClientError && error.kind === 'forbidden') {
        set({
          activeTargetUsername: username,
          editedUser: null,
          isLoadingSettings: false,
          settingsLoadError: '',
        })
        return { kind: 'forbidden' }
      }

      if (error instanceof AuthClientError && error.kind === 'not-found') {
        set({
          activeTargetUsername: username,
          editedUser: null,
          isLoadingSettings: false,
          settingsLoadError: 'User not found.',
        })
        return { kind: 'not-found' }
      }

      set({
        activeTargetUsername: username,
        editedUser: null,
        isLoadingSettings: false,
        settingsLoadError: 'Unable to load settings.',
      })
      return { kind: 'failed' }
    }
  },
  setEditedUser: (username, editedUser) =>
    set((state) =>
      state.activeTargetUsername && sameUsername(state.activeTargetUsername, username)
        ? {
            editedUser,
            settingsLoadError: '',
          }
        : state,
    ),
  reset: () =>
    set({
      activeTargetUsername: null,
      editedUser: null,
      isLoadingSettings: false,
      settingsLoadError: '',
    }),
}))
