import type { NewUserGroupMemberRole } from '@/objects/usergroup/request/NewUserGroupMemberRole'
import type { UserGroupDetail } from '@/objects/usergroup/response/UserGroupDetail'
import { userGroupDescriptionValue } from '@/objects/usergroup/UserGroupDescription'
import { userGroupNameValue } from '@/objects/usergroup/UserGroupName'

/**
 * 用户组详情编辑器状态，保存组名、描述和添加成员草稿。
 */
export type UserGroupEditorState = {
  name: string
  description: string
  memberUsername: string
  memberRole: NewUserGroupMemberRole
}

/**
 * 用户组详情编辑器动作，覆盖详情水合、字段编辑和清空成员草稿。
 */
export type UserGroupEditorAction =
  | { type: 'hydrate'; userGroup: UserGroupDetail | null }
  | { type: 'set_name'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_member_username'; value: string }
  | { type: 'set_member_role'; value: NewUserGroupMemberRole }
  | { type: 'clear_member_draft' }

/**
 * 用户组详情编辑器初始状态，默认新增成员角色为普通成员。
 */
export const initialUserGroupEditorState: UserGroupEditorState = {
  name: '',
  description: '',
  memberUsername: '',
  memberRole: 'member',
}

/**
 * 用户组详情编辑器 reducer；纯函数维护表单输入，不执行网络请求。
 */
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
