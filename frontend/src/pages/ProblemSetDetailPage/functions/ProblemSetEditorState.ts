import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import { grantedGroupsInputFromAccessPolicy, grantedUsersInputFromAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

export type ProblemSetEditorState = {
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  linkProblemSlug: string
}

export type ProblemSetEditorAction =
  | { type: 'hydrate'; problemSet: ProblemSetDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_link_problem_slug'; value: string }
  | { type: 'clear_link_problem_slug' }

export const initialProblemSetEditorState: ProblemSetEditorState = {
  title: '',
  description: '',
  baseAccess: 'restricted',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  linkProblemSlug: '',
}

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
            baseAccess: action.problemSet.accessPolicy.baseAccess,
            grantedUsersInput: grantedUsersInputFromAccessPolicy(action.problemSet.accessPolicy),
            grantedGroupsInput: grantedGroupsInputFromAccessPolicy(action.problemSet.accessPolicy),
          }
        : initialProblemSetEditorState
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_description':
      return { ...state, description: action.value }
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
