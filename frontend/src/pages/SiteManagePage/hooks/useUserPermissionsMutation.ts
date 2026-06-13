import { useCallback, useState } from 'react'

import type { Username } from '@/objects/user/Username'
import { UpdateAccountPermissions } from '@/apis/auth/UpdateAccountPermissions'
import { isHttpClientError } from '@/system/api/http-client'
import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'
import type { UpdateUserPermissionsRequest } from '@/objects/auth/request/UpdateUserPermissionsRequest'
import type { NavigationIntent } from '@/pages/routing/NavigationIntent'
import { toSiteManageDeniedRedirect } from '@/pages/routing/RoutePolicy'
import { sendAPI } from '@/system/api/api-message'
import { translateMessage } from '@/system/i18n/messages'

/**
 * 用户权限保存结果，成功携带保存提示，失败携带错误消息。
 */
type SavePermissionsResult =
  | { kind: 'updated'; user: ManagedUserListItem }
  | { kind: 'forbidden' }
  | { kind: 'failed'; message: string }

/**
 * 用户权限保存 hook；提交权限更新请求并返回标准化结果。
 */
export function useUserPermissionsMutation() {
  const [updatingUsername, setUpdatingUsername] = useState<Username | null>(null)
  const [navigationIntent, setNavigationIntent] = useState<NavigationIntent | null>(null)

  const savePermissions = useCallback(
    async (targetUsername: Username, nextPermissions: UpdateUserPermissionsRequest): Promise<SavePermissionsResult> => {
      setUpdatingUsername(targetUsername)
      setNavigationIntent(null)

      try {
        const updatedUser = await sendAPI(new UpdateAccountPermissions(targetUsername, nextPermissions))
        setUpdatingUsername(null)
        return { kind: 'updated', user: updatedUser }
      } catch (error) {
        if (isHttpClientError(error) && error.kind === 'forbidden') {
          setUpdatingUsername(null)
          setNavigationIntent(toSiteManageDeniedRedirect())
          return { kind: 'forbidden' }
        }

        const message = translateMessage('siteManage.message.updatePermissionsFailed')
        setUpdatingUsername(null)
        return { kind: 'failed', message }
      }
    },
    [],
  )

  return {
    updatingUsername,
    navigationIntent,
    savePermissions,
  }
}
