import { useCallback, useState } from 'react'

import { deleteUserGroup } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { HttpClientError } from '@/shared/api/http-client'

export function useUserGroupDeleteAction(userGroupSlug: UserGroupSlug) {
  const [isDeleting, setIsDeleting] = useState(false)

  const deleteCurrentUserGroup = useCallback(async (): Promise<{ ok: true; message: string } | { ok: false; message: string }> => {
    setIsDeleting(true)
    try {
      const response = await deleteUserGroup(userGroupSlug)
      return { ok: true, message: response.message }
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to delete user group.'
      return { ok: false, message }
    } finally {
      setIsDeleting(false)
    }
  }, [userGroupSlug])

  return { isDeleting, deleteCurrentUserGroup }
}
