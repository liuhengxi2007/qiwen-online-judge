import { useEffect, useRef, useState } from 'react'

import { AuthClientError, getUserProfile } from '@/features/auth/api/auth-client'
import type { UserProfileResponse, Username } from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toForbiddenRedirect } from '@/features/auth/lib/route-policy'

type UseUserProfileQueryArgs = {
  targetUsername: Username
}

export function useUserProfileQuery({ targetUsername }: UseUserProfileQueryArgs) {
  const [activeTargetUsername, setActiveTargetUsername] = useState<Username | null>(null)
  const [profile, setProfile] = useState<UserProfileResponse | null>(null)
  const [isLoadingProfile, setIsLoadingProfile] = useState(false)
  const [profileLoadError, setProfileLoadError] = useState('')
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)
  const requestIdRef = useRef(0)

  useEffect(() => {
    let isCancelled = false
    requestIdRef.current += 1
    const nextRequestId = requestIdRef.current
    setActiveTargetUsername(targetUsername)
    setProfile(null)
    setIsLoadingProfile(true)
    setProfileLoadError('')
    setNavigationIntent(null)

    void getUserProfile(targetUsername)
      .then((loadedProfile) => {
        if (isCancelled || requestIdRef.current !== nextRequestId) {
          return
        }

        setProfile(loadedProfile)
        setIsLoadingProfile(false)
      })
      .catch((error: unknown) => {
        if (isCancelled || requestIdRef.current !== nextRequestId) {
          return
        }

        setProfile(null)
        setIsLoadingProfile(false)

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setProfileLoadError('')
          setNavigationIntent(toForbiddenRedirect())
          return
        }

        if (error instanceof AuthClientError && error.kind === 'not-found') {
          setProfileLoadError('User not found.')
          return
        }

        setProfileLoadError('Unable to load profile.')
      })

    return () => {
      isCancelled = true
    }
  }, [targetUsername])

  return {
    profile: activeTargetUsername === targetUsername ? profile : null,
    isLoadingProfile,
    profileLoadError,
    navigationIntent,
  }
}
