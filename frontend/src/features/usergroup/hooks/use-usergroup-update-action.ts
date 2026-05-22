import { useCallback, useState } from 'react'

import { updateUserGroup } from '@/features/usergroup/http/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { validateUserGroupUpdateDraft } from '@/features/usergroup/domain/usergroup-form'
import { HttpClientError } from '@/shared/api/http-client'
import { useI18n } from '@/shared/i18n/use-i18n'

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
