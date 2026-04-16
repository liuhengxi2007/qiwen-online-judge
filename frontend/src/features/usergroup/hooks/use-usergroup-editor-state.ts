import { useEffect, useReducer } from 'react'

import type { AddUserGroupMemberRole, UserGroupDetail } from '@/features/usergroup/domain/usergroup'
import {
  initialUserGroupEditorState,
  reduceUserGroupEditorState,
} from '@/features/usergroup/domain/usergroup-editor-state'

export function useUserGroupEditorState(userGroup: UserGroupDetail | null) {
  const [state, dispatch] = useReducer(reduceUserGroupEditorState, initialUserGroupEditorState)

  useEffect(() => {
    dispatch({ type: 'hydrate', userGroup })
  }, [userGroup?.id])

  return {
    ...state,
    setName: (value: string) => dispatch({ type: 'set_name', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setMemberUsername: (value: string) => dispatch({ type: 'set_member_username', value }),
    setMemberRole: (value: AddUserGroupMemberRole) => dispatch({ type: 'set_member_role', value }),
    clearMemberDraft: () => dispatch({ type: 'clear_member_draft' }),
  }
}
