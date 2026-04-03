import { useCallback, useState } from 'react'

import { updateUserGroup } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { validateUserGroupUpdateDraft } from '@/features/usergroup/domain/usergroup-form'
import { HttpClientError } from '@/shared/api/http-client'

export function useUserGroupUpdateAction(userGroupSlug: UserGroupSlug) {
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
        const updatedUserGroup = await updateUserGroup(userGroupSlug, validation.request)
        return { ok: true, userGroup: updatedUserGroup, message: 'User group updated successfully.' }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to update user group.'
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [userGroupSlug],
  )

  return { isSaving, save }
}
