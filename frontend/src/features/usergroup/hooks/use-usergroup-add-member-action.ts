import { useCallback, useState } from 'react'

import { addUserGroupMember } from '@/features/usergroup/http/api/usergroup-client'
import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'
import type { UserGroupDetail } from '@/features/usergroup/http/response/UserGroupDetail'
import type { UserGroupSlug } from '@/features/usergroup/model/UserGroupSlug'
import { validateAddUserGroupMemberDraft } from '@/features/usergroup/lib/usergroup-form'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/use-i18n'

export function useUserGroupAddMemberAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [isAddingMember, setIsAddingMember] = useState(false)

  const addMember = useCallback(
    async (
      username: string,
      role: AddUserGroupMemberRole,
    ): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateAddUserGroupMemberDraft(username, role)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsAddingMember(true)
      try {
        const updatedUserGroup = await addUserGroupMember(userGroupSlug, validation.request)
        return { ok: true, userGroup: updatedUserGroup, message: t('userGroup.message.addMemberSuccess') }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('userGroup.message.addMemberFailed')
        return { ok: false, message }
      } finally {
        setIsAddingMember(false)
      }
    },
    [userGroupSlug, t],
  )

  return { isAddingMember, addMember }
}
