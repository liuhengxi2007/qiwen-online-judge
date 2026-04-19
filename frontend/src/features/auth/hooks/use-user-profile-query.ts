import { useEffect, useRef, useState } from 'react'

import { AuthClientError, getUserProfile } from '@/features/auth/api/auth-client'
import type { UserProfileResponse, Username } from '@/features/auth/domain/auth'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toForbiddenRedirect } from '@/features/auth/lib/route-policy'

type UseUserProfileQueryArgs = {
  targetUsername: Username
}

export function useUserProfileQuery({ targetUsername }: UseUserProfileQueryArgs) {
  const [profileState, setProfileState] = useState<{
    username: Username | null
    profile: UserProfileResponse | null
    profileLoadError: string
    navigationIntent: NavigationIntent | null
  }>({
    username: null,
    profile: null,
    profileLoadError: '',
    navigationIntent: null,
  })
  const requestIdRef = useRef(0)

  useEffect(() => {
    let isCancelled = false
    requestIdRef.current += 1
    const nextRequestId = requestIdRef.current

    void getUserProfile(targetUsername)
      .then((loadedProfile) => {
        if (isCancelled || requestIdRef.current !== nextRequestId) {
          return
        }

        setProfileState({
          username: targetUsername,
          profile: loadedProfile,
          profileLoadError: '',
          navigationIntent: null,
        })
      })
      .catch((error: unknown) => {
        if (isCancelled || requestIdRef.current !== nextRequestId) {
          return
        }

        if (error instanceof AuthClientError && error.kind === 'forbidden') {
          setProfileState({
            username: targetUsername,
            profile: null,
            profileLoadError: '',
            navigationIntent: toForbiddenRedirect(),
          })
          return
        }

        if (error instanceof AuthClientError && error.kind === 'not-found') {
          setProfileState({
            username: targetUsername,
            profile: null,
            profileLoadError: 'User not found.',
            navigationIntent: null,
          })
          return
        }

        setProfileState({
          username: targetUsername,
          profile: null,
          profileLoadError: 'Unable to load profile.',
          navigationIntent: null,
        })
      })

    return () => {
      isCancelled = true
    }
  }, [targetUsername])

  return {
    profile: profileState.username === targetUsername ? profileState.profile : null,
    isLoadingProfile: profileState.username !== targetUsername,
    profileLoadError: profileState.username === targetUsername ? profileState.profileLoadError : '',
    navigationIntent: profileState.username === targetUsername ? profileState.navigationIntent : null,
  }
}
