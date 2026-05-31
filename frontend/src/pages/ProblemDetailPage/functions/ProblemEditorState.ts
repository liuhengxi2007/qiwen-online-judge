import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/components/ResourceAccessEditorInput'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

export type ProblemEditorState = {
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  otherUserSubmissionAccess: OtherUserSubmissionAccess
}

export type ProblemEditorAction =
  | { type: 'hydrate'; problem: ProblemDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_manager_users_input'; value: string }
  | { type: 'set_manager_groups_input'; value: string }
  | { type: 'set_other_user_submission_access'; value: OtherUserSubmissionAccess }

export const initialProblemEditorState: ProblemEditorState = {
  title: '',
  statement: '',
  baseAccess: 'restricted',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  managerUsersInput: '',
  managerGroupsInput: '',
  otherUserSubmissionAccess: 'none',
}

export function hydrateProblemEditorState(problem: ProblemDetail | null): ProblemEditorState {
  return problem
    ? {
        title: problem.title,
        statement: problem.statement,
        baseAccess: problem.accessPolicy.baseAccess,
        grantedUsersInput: grantedUsersInputFromAccessPolicy(problem.accessPolicy),
        grantedGroupsInput: grantedGroupsInputFromAccessPolicy(problem.accessPolicy),
        managerUsersInput: grantedManagerUsersInputFromAccessPolicy(problem.accessPolicy),
        managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(problem.accessPolicy),
        otherUserSubmissionAccess: problem.otherUserSubmissionAccess,
      }
    : initialProblemEditorState
}

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
