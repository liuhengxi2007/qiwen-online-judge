import { useCallback, useState } from 'react'

import { UpdateUserGroupMemberRole } from '@/apis/usergroup/UpdateUserGroupMemberRole'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupRole } from '@/objects/usergroup/UserGroupRole'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import type { Username } from '@/objects/user/Username'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

export function useUserGroupUpdateMemberRoleAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [activeUpdatingUsername, setActiveUpdatingUsername] = useState<Username | null>(null)

  const updateRole = useCallback(
    async (
      targetUsername: Username,
      role: UserGroupRole,
    ): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      setActiveUpdatingUsername(targetUsername)
      try {
        const updatedUserGroup = await sendAPI(new UpdateUserGroupMemberRole(userGroupSlug, targetUsername, { role }))
        return {
          ok: true,
          userGroup: updatedUserGroup,
          message: role === 'owner' ? t('userGroup.message.transferOwnershipSuccess') : t('userGroup.message.memberRoleUpdatedSuccess'),
        }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('userGroup.message.updateMemberRoleFailed')
        return { ok: false, message }
      } finally {
        setActiveUpdatingUsername(null)
      }
    },
    [userGroupSlug, t],
  )

  return { activeUpdatingUsername, updateRole }
}
