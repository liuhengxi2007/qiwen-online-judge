import { useEffect, useReducer } from 'react'

import type { NewUserGroupMemberRole } from '@/objects/usergroup/request/NewUserGroupMemberRole'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import {
  initialUserGroupEditorState,
  reduceUserGroupEditorState,
} from '../functions/UserGroupEditorState'

/**
 * 用户组编辑器状态 hook，根据加载到的用户组详情水合名称、描述和新增成员草稿。
 */
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
    setMemberRole: (value: NewUserGroupMemberRole) => dispatch({ type: 'set_member_role', value }),
    clearMemberDraft: () => dispatch({ type: 'clear_member_draft' }),
  }
}
