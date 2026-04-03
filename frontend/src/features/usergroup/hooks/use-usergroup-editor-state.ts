import { useEffect, useState } from 'react'

import type { UserGroupDetail, UserGroupRole } from '@/features/usergroup/domain/usergroup'
import { userGroupDescriptionValue, userGroupNameValue } from '@/features/usergroup/domain/usergroup'

export function useUserGroupEditorState(userGroup: UserGroupDetail | null) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [memberUsername, setMemberUsername] = useState('')
  const [memberRole, setMemberRole] = useState<UserGroupRole>('member')

  useEffect(() => {
    if (!userGroup) {
      return
    }

    setName(userGroupNameValue(userGroup.name))
    setDescription(userGroupDescriptionValue(userGroup.description))
  }, [userGroup])

  function clearMemberDraft() {
    setMemberUsername('')
    setMemberRole('member')
  }

  return {
    name,
    description,
    memberUsername,
    memberRole,
    setName,
    setDescription,
    setMemberUsername,
    setMemberRole,
    clearMemberDraft,
  }
}
