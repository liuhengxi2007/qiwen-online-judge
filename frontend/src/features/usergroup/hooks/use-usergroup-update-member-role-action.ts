import { useCallback, useState } from 'react'

import { updateUserGroupMemberRole } from '@/features/usergroup/api/usergroup-client'
import type { UserGroupDetail, UserGroupRole, UserGroupSlug } from '@/features/usergroup/domain/usergroup'
import type { Username } from '@/features/auth/domain/auth'
import { HttpClientError } from '@/shared/api/http-client'

export function useUserGroupUpdateMemberRoleAction(userGroupSlug: UserGroupSlug) {
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
          message: role === 'owner' ? 'Ownership transferred successfully.' : 'Member role updated successfully.',
        }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to update member role.'
        return { ok: false, message }
      } finally {
        setActiveUpdatingUsername(null)
      }
    },
    [userGroupSlug],
  )

  return { activeUpdatingUsername, updateRole }
}
