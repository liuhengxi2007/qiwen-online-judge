import { useEffect, useRef, useState } from 'react'

import type { Username } from '@/features/user/model/Username'
import type { NavigationIntent } from '@/shared/routing/navigation-intent'
import { toForbiddenRedirect } from '@/features/auth/lib/route-policy'
import { UserClientError, getUserProfile } from '@/features/user/http/api/user-client'
import type { UserProfileResponse } from '@/features/user/http/response/UserProfileResponse'
import { translateMessage } from '@/shared/i18n/messages'

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

        if (error instanceof UserClientError && error.kind === 'forbidden') {
          setProfileState({
            username: targetUsername,
            profile: null,
            profileLoadError: '',
            navigationIntent: toForbiddenRedirect(),
          })
          return
        }

        if (error instanceof UserClientError && error.kind === 'not-found') {
          setProfileState({
            username: targetUsername,
            profile: null,
            profileLoadError: translateMessage('api.error.user.not_found'),
            navigationIntent: null,
          })
          return
        }

        setProfileState({
          username: targetUsername,
          profile: null,
          profileLoadError: translateMessage('userProfile.loadFailed'),
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
