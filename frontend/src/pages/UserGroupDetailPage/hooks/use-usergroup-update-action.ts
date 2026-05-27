import { useCallback, useState } from 'react'

import { updateUserGroup } from '@/apis/usergroup/UpdateUserGroup'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { validateUserGroupUpdateDraft } from '@/pages/objects/usergroup-form'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

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
        const updatedUserGroup = await updateUserGroup(userGroupSlug, validation.request)
        return { ok: true, userGroup: updatedUserGroup, message: t('userGroup.message.updateSuccess') }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('userGroup.message.updateFailed')
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [userGroupSlug, t],
  )

  return { isSaving, save }
}
