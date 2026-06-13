import { useCallback, useState } from 'react'

import { DeleteUserGroup } from '@/apis/usergroup/DeleteUserGroup'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组删除操作 hook，封装删除 API 和按钮 loading 状态。
 * 成功时只返回消息，页面层负责关闭/跳转，不在 hook 内直接导航。
 */
export function useUserGroupDeleteAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentUserGroup = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      const response = await sendAPI(new DeleteUserGroup(userGroupSlug))
      return { ok: true, message: response.message ?? t('common.success.generic') }
    } catch (error) {
      const message = isHttpClientError(error) ? error.message : t('userGroup.message.deleteFailed')
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [userGroupSlug, t])

  return { isDeleting, deleteCurrentUserGroup }
}
