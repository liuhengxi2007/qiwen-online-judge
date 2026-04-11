import { useEffect, useReducer } from 'react'

import type { OthersSubmissionAccess, ProblemDetail } from '@/features/problem/domain/problem'
import {
  grantedGroupsInputFromAccessPolicy,
  grantedManagerGroupsInputFromAccessPolicy,
  grantedManagerUsersInputFromAccessPolicy,
  grantedUsersInputFromAccessPolicy,
} from '@/shared/domain/resource-access-input'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

type ProblemEditorState = {
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

type ProblemEditorAction =
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

const initialState: ProblemEditorState = {
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

function reducer(state: ProblemEditorState, action: ProblemEditorAction): ProblemEditorState {
  switch (action.type) {
    case 'hydrate':
      return action.problem
        ? {
            title: action.problem.title,
            statement: action.problem.statement,
            timeLimitMs: action.problem.timeLimitMs,
            spaceLimitMb: action.problem.spaceLimitMb,
            baseAccess: action.problem.accessPolicy.baseAccess,
            grantedUsersInput: grantedUsersInputFromAccessPolicy(action.problem.accessPolicy),
            grantedGroupsInput: grantedGroupsInputFromAccessPolicy(action.problem.accessPolicy),
            managerUsersInput: grantedManagerUsersInputFromAccessPolicy(action.problem.accessPolicy),
            managerGroupsInput: grantedManagerGroupsInputFromAccessPolicy(action.problem.accessPolicy),
            othersSubmissionAccess: action.problem.othersSubmissionAccess,
          }
        : initialState
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

export function useProblemEditorState(problem: ProblemDetail | null) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    dispatch({ type: 'hydrate', problem })
  }, [problem?.id])

  return {
    ...state,
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setStatement: (value: string) => dispatch({ type: 'set_statement', value }),
    setTimeLimitMs: (value: number) => dispatch({ type: 'set_time_limit_ms', value }),
    setSpaceLimitMb: (value: number) => dispatch({ type: 'set_space_limit_mb', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    setManagerUsersInput: (value: string) => dispatch({ type: 'set_manager_users_input', value }),
    setManagerGroupsInput: (value: string) => dispatch({ type: 'set_manager_groups_input', value }),
    setOthersSubmissionAccess: (value: OthersSubmissionAccess) => dispatch({ type: 'set_others_submission_access', value }),
  }
}
