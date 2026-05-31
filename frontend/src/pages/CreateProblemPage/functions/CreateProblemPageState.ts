import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

export type CreateProblemPageDraft = {
  slug: string
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  otherUserSubmissionAccess: OtherUserSubmissionAccess
}

export type CreateProblemPageState = {
  isSubmitting: boolean
  draft: CreateProblemPageDraft
  errorMessage: string
  successMessage: string
}

export type CreateProblemPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_manager_users_input'; value: string }
  | { type: 'set_manager_groups_input'; value: string }
  | { type: 'set_other_user_submission_access'; value: OtherUserSubmissionAccess }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded'; message: string }
  | { type: 'submit_failed'; message: string }

export const initialCreateProblemPageDraft: CreateProblemPageDraft = {
  slug: '',
  title: '',
  statement: '',
  baseAccess: 'restricted',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  managerUsersInput: '',
  managerGroupsInput: '',
  otherUserSubmissionAccess: 'none',
}

export const initialCreateProblemPageState: CreateProblemPageState = {
  isSubmitting: false,
  draft: initialCreateProblemPageDraft,
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
    draft: initialCreateProblemPageDraft,
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
      return { ...state, draft: { ...state.draft, slug: action.value } }
    case 'set_title':
      return { ...state, draft: { ...state.draft, title: action.value } }
    case 'set_statement':
      return { ...state, draft: { ...state.draft, statement: action.value } }
    case 'set_base_access':
      return { ...state, draft: { ...state.draft, baseAccess: action.value } }
    case 'set_granted_users_input':
      return { ...state, draft: { ...state.draft, grantedUsersInput: action.value } }
    case 'set_granted_groups_input':
      return { ...state, draft: { ...state.draft, grantedGroupsInput: action.value } }
    case 'set_manager_users_input':
      return { ...state, draft: { ...state.draft, managerUsersInput: action.value } }
    case 'set_manager_groups_input':
      return { ...state, draft: { ...state.draft, managerGroupsInput: action.value } }
    case 'set_other_user_submission_access':
      return { ...state, draft: { ...state.draft, otherUserSubmissionAccess: action.value } }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return resetCreateProblemPageState(state, action.message)
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}
