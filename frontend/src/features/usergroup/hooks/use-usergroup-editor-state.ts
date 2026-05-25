import { useEffect, useReducer } from 'react'

import type { AddUserGroupMemberRole } from '@/features/usergroup/model/AddUserGroupMemberRole'
import type { UserGroupDetail } from '@/features/usergroup/model/response/UserGroupDetail'
import {
  initialUserGroupEditorState,
  reduceUserGroupEditorState,
} from '@/features/usergroup/state/usergroup-editor-state'

export function useUserGroupEditorState(userGroup: UserGroupDetail | null) {
  const [state, dispatch] = useReducer(reduceUserGroupEditorState, initialUserGroupEditorState)

  useEffect(() => {
    dispatch({ type: 'hydrate', userGroup })
  }, [userGroup])

  return {
    ...state,
    setName: (value: string) => dispatch({ type: 'set_name', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setMemberUsername: (value: string) => dispatch({ type: 'set_member_username', value }),
    setMemberRole: (value: AddUserGroupMemberRole) => dispatch({ type: 'set_member_role', value }),
    clearMemberDraft: () => dispatch({ type: 'clear_member_draft' }),
  }
}
