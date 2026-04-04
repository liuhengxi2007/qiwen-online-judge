import { useEffect, useReducer } from 'react'

import type { ProblemDetail } from '@/features/problem/domain/problem'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

type ProblemEditorState = {
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

type ProblemEditorAction =
  | { type: 'hydrate'; problem: ProblemDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }

const initialState: ProblemEditorState = {
  title: '',
  statement: '',
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
}

function reducer(state: ProblemEditorState, action: ProblemEditorAction): ProblemEditorState {
  switch (action.type) {
    case 'hydrate':
      return action.problem
        ? {
            title: action.problem.title,
            statement: action.problem.statement,
            baseAccess: action.problem.accessPolicy.baseAccess,
            grantedUsersInput: action.problem.accessPolicy.viewerGrants
              .filter((grant) => grant.kind === 'user')
              .map((grant) => grant.username)
              .join('\n'),
            grantedGroupsInput: action.problem.accessPolicy.viewerGrants
              .filter((grant) => grant.kind === 'user_group')
              .map((grant) => grant.slug)
              .join('\n'),
          }
        : initialState
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
  }
}

export function useProblemEditorState(problem: ProblemDetail | null) {
  const [state, dispatch] = useReducer(reducer, initialState)

  useEffect(() => {
    dispatch({ type: 'hydrate', problem })
  }, [problem])

  return {
    ...state,
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setStatement: (value: string) => dispatch({ type: 'set_statement', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
  }
}
