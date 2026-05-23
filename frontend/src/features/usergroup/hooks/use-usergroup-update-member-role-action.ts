import { useCallback, useState } from 'react'

import { updateUserGroupMemberRole } from '@/features/usergroup/http/api/usergroup-client'
import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import type { UserGroupRole } from '@/features/usergroup/model/UserGroupRole'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import type { Username } from '@/features/user/model/Username'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/use-i18n'

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
        const updatedUserGroup = await updateUserGroupMemberRole(userGroupSlug, targetUsername, { role })
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
