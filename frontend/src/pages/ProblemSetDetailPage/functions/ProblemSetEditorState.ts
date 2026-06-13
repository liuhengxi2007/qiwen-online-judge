import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import { usernameValue } from '@/objects/user/Username'
import { grantedGroupsInputFromAccessPolicy, grantedUsersInputFromAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

/**
 * 题单详情编辑器状态，保存内容、访问控制和关联题目输入。
 */
export type ProblemSetEditorState = {
  title: string
  description: string
  authorUsername: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  linkProblemSlug: string
}

/**
 * 题单详情编辑器动作，覆盖水合、内容编辑、访问控制编辑和清空关联输入。
 */
export type ProblemSetEditorAction =
  | { type: 'hydrate'; problemSet: ProblemSetDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_author_username'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_link_problem_slug'; value: string }
  | { type: 'clear_link_problem_slug' }

/**
 * 题单详情编辑器初始状态，默认限制访问且没有授权输入。
 */
export const initialProblemSetEditorState: ProblemSetEditorState = {
  title: '',
  description: '',
  authorUsername: '',
  baseAccess: 'restricted',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  linkProblemSlug: '',
}

/**
 * 题单详情编辑器 reducer；纯函数维护编辑草稿。
 */
export function reduceProblemSetEditorState(
  state: ProblemSetEditorState,
  action: ProblemSetEditorAction,
): ProblemSetEditorState {
  switch (action.type) {
    case 'hydrate':
      return action.problemSet
        ? {
            ...state,
            title: action.problemSet.title,
            description: action.problemSet.description,
            authorUsername: action.problemSet.author ? usernameValue(action.problemSet.author.username) : '',
            baseAccess: action.problemSet.accessPolicy.baseAccess,
            grantedUsersInput: grantedUsersInputFromAccessPolicy(action.problemSet.accessPolicy),
            grantedGroupsInput: grantedGroupsInputFromAccessPolicy(action.problemSet.accessPolicy),
          }
        : initialProblemSetEditorState
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_description':
      return { ...state, description: action.value }
    case 'set_author_username':
      return { ...state, authorUsername: action.value }
    case 'set_base_access':
      return { ...state, baseAccess: action.value }
    case 'set_granted_users_input':
      return { ...state, grantedUsersInput: action.value }
    case 'set_granted_groups_input':
      return { ...state, grantedGroupsInput: action.value }
    case 'set_link_problem_slug':
      return { ...state, linkProblemSlug: action.value }
    case 'clear_link_problem_slug':
      return { ...state, linkProblemSlug: '' }
  }
}
