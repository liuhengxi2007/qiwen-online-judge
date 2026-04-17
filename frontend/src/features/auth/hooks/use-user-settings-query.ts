import { useEffect, useRef, useState } from 'react'

import { AuthClientError, getUserSettings } from '@/features/auth/api/auth-client'
import type { SessionResponse, Username } from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toForbiddenRedirect } from '@/features/auth/lib/route-policy'

type UseUserSettingsQueryArgs = {
  canLoadTarget: boolean
  targetUsername: Username
}

export function useUserSettingsQuery({ canLoadTarget, targetUsername }: UseUserSettingsQueryArgs) {
  const [activeTargetUsername, setActiveTargetUsername] = useState<Username | null>(null)
  const [editedUser, setEditedUser] = useState<SessionResponse | null>(null)
  const [isLoadingSettings, setIsLoadingSettings] = useState(false)
  const [settingsLoadError, setSettingsLoadError] = useState('')
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)
  const requestIdRef = useRef(0)

  useEffect(() => {
    if (!canLoadTarget) {
      setActiveTargetUsername(null)
      setEditedUser(null)
      setIsLoadingSettings(false)
      setSettingsLoadError('')
      setNavigationIntent(null)
      return
    }

    let isCancelled = false
    requestIdRef.current += 1
    const nextRequestId = requestIdRef.current
    setActiveTargetUsername(targetUsername)
    setEditedUser(null)
    setIsLoadingSettings(true)
    setSettingsLoadError('')
    setNavigationIntent(null)

    void getUserSettings(targetUsername)
      .then((loadedUser) => {
        if (isCancelled) {
          return
        }

        if (requestIdRef.current != nextRequestId) {
          return
        }

        setEditedUser(loadedUser)
        setIsLoadingSettings(false)
      })
      .catch((error: unknown) => {
        if (isCancelled) {
          return
        }

        if (requestIdRef.current != nextRequestId) {
          return
        }

        setEditedUser(null)
        setIsLoadingSettings(false)

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setSettingsLoadError('')
          setNavigationIntent(toForbiddenRedirect())
          return
        }

        if (error instanceof AuthClientError && error.kind === 'not-found') {
          setSettingsLoadError('User not found.')
          return
        }

        setSettingsLoadError('Unable to load settings.')
      })

    return () => {
      isCancelled = true
    }
  }, [canLoadTarget, targetUsername])

  return {
    editedUser: activeTargetUsername === targetUsername ? editedUser : null,
    isLoadingSettings,
    settingsLoadError,
    navigationIntent,
    replaceEditedUser(username: Username, nextUser: SessionResponse) {
      if (activeTargetUsername !== username) {
        return
      }

      setEditedUser(nextUser)
      setSettingsLoadError('')
    },
  }
}
