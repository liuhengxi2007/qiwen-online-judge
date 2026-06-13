import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import { usernameValue } from '@/objects/user/Username'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/components/ResourceAccessEditorInput'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

/**
 * 题目详情编辑器状态，保存内容字段、访问控制输入和弹窗状态。
 */
export type ProblemEditorState = {
  title: string
  statement: string
  authorUsername: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  otherUserSubmissionAccess: OtherUserSubmissionAccess
}

/**
 * 题目详情编辑器动作，覆盖水合、内容编辑、访问控制编辑和弹窗开关。
 */
export type ProblemEditorAction =
  | { type: 'hydrate'; problem: ProblemDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_author_username'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_manager_users_input'; value: string }
  | { type: 'set_manager_groups_input'; value: string }
  | { type: 'set_other_user_submission_access'; value: OtherUserSubmissionAccess }

/**
 * 题目详情编辑器初始状态，默认访问控制为受限且弹窗关闭。
 */
export const initialProblemEditorState: ProblemEditorState = {
  title: '',
  statement: '',
  authorUsername: '',
  baseAccess: 'restricted',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  managerUsersInput: '',
  managerGroupsInput: '',
  otherUserSubmissionAccess: 'none',
}

/**
 * 从题目详情水合编辑器状态；详情为空时返回初始状态。
 */
export function hydrateProblemEditorState(problem: ProblemDetail | null): ProblemEditorState {
  return problem
    ? {
        title: problem.title,
        statement: problem.statement,
        authorUsername: problem.author ? usernameValue(problem.author.username) : '',
        baseAccess: problem.accessPolicy.baseAccess,
        grantedUsersInput: grantedUsersInputFromAccessPolicy(problem.accessPolicy),
        grantedGroupsInput: grantedGroupsInputFromAccessPolicy(problem.accessPolicy),
        managerUsersInput: grantedManagerUsersInputFromAccessPolicy(problem.accessPolicy),
        managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(problem.accessPolicy),
        otherUserSubmissionAccess: problem.otherUserSubmissionAccess,
      }
    : initialProblemEditorState
}

/**
 * 题目详情编辑器 reducer；纯函数维护编辑草稿和弹窗状态。
 */
export function reduceProblemEditorState(
  state: ProblemEditorState,
  action: ProblemEditorAction,
): ProblemEditorState {
  switch (action.type) {
    case 'hydrate':
      return hydrateProblemEditorState(action.problem)
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_statement':
      return { ...state, statement: action.value }
    case 'set_author_username':
      return { ...state, authorUsername: action.value }
    case 'set_base_access':
      return { ...state, baseAccess: action.value }
    case 'set_granted_users_input':
      return { ...state, grantedUsersInput: action.value }
    case 'set_granted_groups_input':
      return { ...state, grantedGroupsInput: action.value }
    case 'set_manager_users_input':
      return { ...state, managerUsersInput: action.value }
    case 'set_manager_groups_input':
      return { ...state, managerGroupsInput: action.value }
    case 'set_other_user_submission_access':
      return { ...state, otherUserSubmissionAccess: action.value }
  }
}
