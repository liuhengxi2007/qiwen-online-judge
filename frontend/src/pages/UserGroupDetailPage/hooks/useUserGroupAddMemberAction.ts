import { useCallback, useState } from 'react'

import { AddUserGroupMember } from '@/apis/usergroup/AddUserGroupMember'
import type { NewUserGroupMemberRole } from '@/objects/usergroup/request/NewUserGroupMemberRole'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { validateAddUserGroupMemberDraft } from '../functions/UserGroupForm'
import { sendAPI } from '@/system/api/api-message'
import { isHttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 用户组新增成员操作 hook，先校验用户名/角色草稿，再调用新增成员 API。
 * 返回更新后的用户组详情，调用方负责刷新页面状态和清空输入。
 */
export function useUserGroupAddMemberAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [isAddingMember, setIsAddingMember] = useState(false)

  const addMember = useCallback(
    async (
      username: string,
      role: NewUserGroupMemberRole,
    ): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateAddUserGroupMemberDraft(username, role)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsAddingMember(true)
      try {
        const updatedUserGroup = await sendAPI(new AddUserGroupMember(userGroupSlug, validation.request))
        return { ok: true, userGroup: updatedUserGroup, message: t('userGroup.message.addMemberSuccess') }
      } catch (error) {
        const message = isHttpClientError(error) ? error.message : t('userGroup.message.addMemberFailed')
        return { ok: false, message }
      } finally {
        setIsAddingMember(false)
      }
    },
    [userGroupSlug, t],
  )

  return { isAddingMember, addMember }
}
