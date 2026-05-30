import { useEffect, useRef, useState } from 'react'

import { GetUserProfile } from '@/apis/user/GetUserProfile'
import type { Username } from '@/objects/user/Username'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import { toForbiddenRedirect } from '@/pages/routing/RoutePolicy'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import { translateMessage } from '@/system/i18n/messages'

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

    void sendAPI(new GetUserProfile(targetUsername))
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

        if (error instanceof HttpClientError && error.kind === 'forbidden') {
          setProfileState({
            username: targetUsername,
            profile: null,
            profileLoadError: '',
            navigationIntent: toForbiddenRedirect(),
          })
          return
        }

        if (error instanceof HttpClientError && error.kind === 'not-found') {
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
