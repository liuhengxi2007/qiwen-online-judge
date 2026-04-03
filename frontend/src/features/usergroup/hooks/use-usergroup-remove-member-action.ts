import { useCallback, useState } from 'react'

import type { Username } from '@/features/auth/domain/auth'
import { removeUserGroupMember } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import { HttpClientError } from '@/shared/api/http-client'

export function useUserGroupRemoveMemberAction(userGroupSlug: UserGroupSlug) {
  const [activeRemovingUsername, setActiveRemovingUsername] = useState<Username | null>(null)

  const removeMember = useCallback(
    async (targetUsername: Username): Promise<{ ok: true; userGroup: UserGroupDetail; message: string } | { ok: false; message: string }> => {
      setActiveRemovingUsername(targetUsername)
      try {
        const updatedUserGroup = await removeUserGroupMember(userGroupSlug, targetUsername)
        return {
          ok: true,
          userGroup: updatedUserGroup,
          message: 'Member removed successfully.',
        }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to remove member.'
        return { ok: false, message }
      } finally {
        setActiveRemovingUsername(null)
      }
    },
    [userGroupSlug],
  )

  return { activeRemovingUsername, removeMember }
}
