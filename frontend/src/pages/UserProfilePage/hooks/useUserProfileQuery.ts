import { useEffect, useRef, useState } from 'react'

import { GetUserProfile } from '@/apis/user/GetUserProfile'
import type { Username } from '@/objects/user/Username'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import { toForbiddenRedirect } from '@/pages/routing/RoutePolicy'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import type { UserProfileResponse } from '@/objects/user/response/UserProfileResponse'
import { translateMessage } from '@/system/i18n/messages'

/**
 * 用户资料查询 hook 参数，targetUsername 已由路由策略解析为合法领域类型。
 */
type UseUserProfileQueryArgs = {
  targetUsername: Username
}

/**
 * 用户资料查询 hook，加载目标用户公开资料并处理 403/404 的页面语义。
 * 403 返回权限跳转意图，404 留在页面展示未找到消息；过期响应通过 request id 丢弃。
 */
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

        if (isHttpClientError(error) && error.kind === 'forbidden') {
          // 注意：公开资料访问遇到 403 时统一跳转权限页，不在资料页展示资源是否存在的额外信息。
          setProfileState({
            username: targetUsername,
            profile: null,
            profileLoadError: '',
            navigationIntent: toForbiddenRedirect(),
          })
          return
        }

        if (isHttpClientError(error) && error.kind === 'not-found') {
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
    replaceProfile(nextProfile: UserProfileResponse) {
      setProfileState({
        username: targetUsername,
        profile: nextProfile,
        profileLoadError: '',
        navigationIntent: null,
      })
    },
  }
}
