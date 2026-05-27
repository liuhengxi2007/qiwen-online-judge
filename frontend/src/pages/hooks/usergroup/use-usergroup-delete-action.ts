import { useCallback, useState } from 'react'

import { deleteUserGroup } from '@/apis/usergroup/DeleteUserGroup'
import type { UserGroupSlug } from '@/objects/usergroup/UserGroupSlug'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'

export function useUserGroupDeleteAction(userGroupSlug: UserGroupSlug) {
  const { t } = useI18n()
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentUserGroup = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      const response = await deleteUserGroup(userGroupSlug)
      return { ok: true, message: response.message ?? t('common.success.generic') }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('userGroup.message.deleteFailed')
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [userGroupSlug, t])

  return { isDeleting, deleteCurrentUserGroup }
}
