import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/pages/components/resource-access-editor-input'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

export type ProblemEditorState = {
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  othersSubmissionAccess: OthersSubmissionAccess
}

export type ProblemEditorAction =
  | { type: 'hydrate'; problem: ProblemDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_time_limit_ms'; value: number }
  | { type: 'set_space_limit_mb'; value: number }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_manager_users_input'; value: string }
  | { type: 'set_manager_groups_input'; value: string }
  | { type: 'set_others_submission_access'; value: OthersSubmissionAccess }

export const initialProblemEditorState: ProblemEditorState = {
  title: '',
  statement: '',
  timeLimitMs: 1000,
  spaceLimitMb: 256,
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  managerUsersInput: '',
  managerGroupsInput: '',
  othersSubmissionAccess: 'none',
}

export function hydrateProblemEditorState(problem: ProblemDetail | null): ProblemEditorState {
  return problem
    ? {
        title: problem.title,
        statement: problem.statement,
        timeLimitMs: problem.timeLimitMs,
        spaceLimitMb: problem.spaceLimitMb,
        baseAccess: problem.accessPolicy.baseAccess,
        grantedUsersInput: grantedUsersInputFromAccessPolicy(problem.accessPolicy),
        grantedGroupsInput: grantedGroupsInputFromAccessPolicy(problem.accessPolicy),
        managerUsersInput: grantedManagerUsersInputFromAccessPolicy(problem.accessPolicy),
        managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(problem.accessPolicy),
        othersSubmissionAccess: problem.othersSubmissionAccess,
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
    case 'set_time_limit_ms':
      return { ...state, timeLimitMs: action.value }
    case 'set_space_limit_mb':
      return { ...state, spaceLimitMb: action.value }
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
    case 'set_others_submission_access':
      return { ...state, othersSubmissionAccess: action.value }
  }
}
