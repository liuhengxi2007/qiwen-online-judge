import type { OthersSubmissionAccess } from '@/features/problem/model/OthersSubmissionAccess'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

export type CreateProblemPageState = {
  isSubmitting: boolean
  slug: string
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
  errorMessage: string
  successMessage: string
}

export type CreateProblemPageAction =
  | { type: 'set_slug'; value: string }
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
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; message: string }
  | { type: 'submit_failed'; message: string }

export const initialCreateProblemPageState: CreateProblemPageState = {
  isSubmitting: false,
  slug: '',
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
  errorMessage: '',
  successMessage: '',
}

export function resetCreateProblemPageState(
  state: CreateProblemPageState,
  successMessage: string,
): CreateProblemPageState {
  return {
    ...state,
    isSubmitting: false,
    slug: '',
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
    errorMessage: '',
    successMessage,
  }
}

export function reduceCreateProblemPageState(
  state: CreateProblemPageState,
  action: CreateProblemPageAction,
): CreateProblemPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, slug: action.value }
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
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return resetCreateProblemPageState(state, action.message)
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}
