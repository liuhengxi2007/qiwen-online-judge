import { useEffect, useRef, useState } from 'react'

import type { SessionResponse } from '@/features/auth/model/response/SessionResponse'
import type { Username } from '@/features/user/model/Username'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toForbiddenRedirect } from '@/features/auth/lib/route-policy'
import { HttpClientError } from '@/shared/api/http-client'
import { getUserSettings } from '@/features/user/http/api/GetUserSettings'
import { translateMessage } from '@/shared/i18n/messages'

type UseUserSettingsQueryArgs = {
  canLoadTarget: boolean
  targetUsername: Username
}

export function useUserSettingsQuery({ canLoadTarget, targetUsername }: UseUserSettingsQueryArgs) {
  const [settingsState, setSettingsState] = useState<{
    username: Username | null
    editedUser: SessionResponse | null
    settingsLoadError: string
    navigationIntent: NavigationIntent | null
  }>({
    username: null,
    editedUser: null,
    settingsLoadError: '',
    navigationIntent: null,
  })
  const requestIdRef = useRef(0)

  useEffect(() => {
    if (!canLoadTarget) {
      return
    }

    let isCancelled = false
    requestIdRef.current += 1
    const nextRequestId = requestIdRef.current

    void getUserSettings(targetUsername)
      .then((loadedUser) => {
        if (isCancelled) {
          return
        }

        if (requestIdRef.current != nextRequestId) {
          return
        }

        setSettingsState({
          username: targetUsername,
          editedUser: loadedUser,
          settingsLoadError: '',
          navigationIntent: null,
        })
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return
        }

        if (requestIdRef.current != nextRequestId) {
          return
        }

        if (error instanceof HttpClientError && error.kind === 'forbidden') {
          setSettingsState({
            username: targetUsername,
            editedUser: null,
            settingsLoadError: '',
            navigationIntent: toForbiddenRedirect(),
          })
          return
        }

        if (error instanceof HttpClientError && error.kind === 'not-found') {
          setSettingsState({
            username: targetUsername,
            editedUser: null,
            settingsLoadError: translateMessage('api.error.user.not_found'),
            navigationIntent: null,
          })
          return
        }

        setSettingsState({
          username: targetUsername,
          editedUser: null,
          settingsLoadError: translateMessage('userSettings.loadFailed'),
          navigationIntent: null,
        })
      })

    return () => {
      isCancelled = true
    }
  }, [canLoadTarget, targetUsername])

  return {
    editedUser: settingsState.username === targetUsername ? settingsState.editedUser : null,
    isLoadingSettings: canLoadTarget && settingsState.username !== targetUsername,
    settingsLoadError: settingsState.username === targetUsername ? settingsState.settingsLoadError : '',
    navigationIntent: settingsState.username === targetUsername ? settingsState.navigationIntent : null,
    replaceEditedUser(username: Username, nextUser: SessionResponse) {
      if (settingsState.username !== username) {
        return
      }

      setSettingsState({
        username,
        editedUser: nextUser,
        settingsLoadError: '',
        navigationIntent: null,
      })
    },
  }
}
