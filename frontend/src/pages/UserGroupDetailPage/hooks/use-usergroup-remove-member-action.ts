import { useCallback, useState } from 'react'

import type { Username } from '@/objects/user/Username'
import { removeUserGroupMember } from '@/apis/usergroup/RemoveUserGroupMember'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

export function useUserGroupRemoveMemberAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [activeRemovingUsername, setActiveRemovingUsername] = useState<Username | null>(null)

  const removeMember = useCallback(
    async (targetUsername: Username): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      setActiveRemovingUsername(targetUsername)
      try {
        const updatedUserGroup = await removeUserGroupMember(userGroupSlug, targetUsername)
        return {
          ok: true,
          userGroup: updatedUserGroup,
          message: t('userGroup.message.removeMemberSuccess'),
        }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('userGroup.message.removeMemberFailed')
        return { ok: false, message }
      } finally {
        setActiveRemovingUsername(null)
      }
    },
    [userGroupSlug, t],
  )

  return { activeRemovingUsername, removeMember }
}
