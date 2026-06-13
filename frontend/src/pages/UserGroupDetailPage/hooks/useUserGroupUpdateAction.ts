import { useCallback, useState } from 'react'

import { UpdateUserGroup } from '@/apis/usergroup/UpdateUserGroup'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { validateUserGroupUpdateDraft } from '../functions/UserGroupForm'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组基本信息保存 hook，校验名称和描述草稿后调用更新 API。
 * 成功结果携带后端返回的完整用户组详情，避免调用方手工拼接状态。
 */
export function useUserGroupUpdateAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [isSaving, setIsSaving] = useState(false)

  const save = useCallback(
    async (draft: {
      name: string
      description: string
    }): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateUserGroupUpdateDraft(draft)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsSaving(true)
      try {
        const updatedUserGroup = await sendAPI(new UpdateUserGroup(userGroupSlug, validation.request))
        return { ok: true, userGroup: updatedUserGroup, message: t('userGroup.message.updateSuccess') }
      } catch (error) {
        const message = isHttpClientError(error) ? error.message : t('userGroup.message.updateFailed')
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [userGroupSlug, t],
  )

  return { isSaving, save }
}
