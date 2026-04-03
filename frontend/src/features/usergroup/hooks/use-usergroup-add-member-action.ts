import { useCallback, useState } from 'react'

import { addUserGroupMember } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { validateAddUserGroupMemberDraft } from '@/features/usergroup/domain/usergroup-form'
import { HttpClientError } from '@/shared/api/http-client'

export function useUserGroupAddMemberAction(userGroupSlug: UserGroupSlug) {
  const [isAddingMember, setIsAddingMember] = useState(false)

  const addMember = useCallback(
    async (username: string, role: string): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateAddUserGroupMemberDraft(username, role)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsAddingMember(true)
      try {
        const updatedUserGroup = await addUserGroupMember(userGroupSlug, validation.request)
        return { ok: true, userGroup: updatedUserGroup, message: 'Member added successfully.' }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to add group member.'
        return { ok: false, message }
      } finally {
        setIsAddingMember(false)
      }
    },
    [userGroupSlug],
  )

  return { isAddingMember, addMember }
}
