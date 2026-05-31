import type { NewUserGroupMemberRole } from '@/objects/usergroup/request/NewUserGroupMemberRole'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import { userGroupDescriptionValue } from '@/objects/usergroup/UserGroupDescription'
import { userGroupNameValue } from '@/objects/usergroup/UserGroupName'

export type UserGroupEditorState = {
  name: string
  description: string
  memberUsername: string
  memberRole: NewUserGroupMemberRole
}

export type UserGroupEditorAction =
  | { type: 'hydrate'; userGroup: UserGroupDetail | null }
  | { type: 'set_name'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_member_username'; value: string }
  | { type: 'set_member_role'; value: NewUserGroupMemberRole }
  | { type: 'clear_member_draft' }

export const initialUserGroupEditorState: UserGroupEditorState = {
  name: '',
  description: '',
  memberUsername: '',
  memberRole: 'member',
}

export function reduceUserGroupEditorState(
  state: UserGroupEditorState,
  action: UserGroupEditorAction,
): UserGroupEditorState {
  switch (action.type) {
    case 'hydrate':
      if (!action.userGroup) {
        return state
      }

      return {
        ...state,
        name: userGroupNameValue(action.userGroup.name),
        description: userGroupDescriptionValue(action.userGroup.description),
      }
    case 'set_name':
      return { ...state, name: action.value }
    case 'set_description':
      return { ...state, description: action.value }
    case 'set_member_username':
      return { ...state, memberUsername: action.value }
    case 'set_member_role':
      return { ...state, memberRole: action.value }
    case 'clear_member_draft':
      return { ...state, memberUsername: '', memberRole: 'member' }
  }
}
