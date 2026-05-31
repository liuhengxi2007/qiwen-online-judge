import { useEffect, useReducer } from 'react'

import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import {
  initialProblemEditorState,
  reduceProblemEditorState,
} from '../functions/ProblemEditorState'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

export function useProblemEditorState(problem: ProblemDetail | null) {
  const [state, dispatch] = useReducer(reduceProblemEditorState, initialProblemEditorState)

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
    setManagerUsersInput: (value: string) => dispatch({ type: 'set_manager_users_input', value }),
    setManagerGroupsInput: (value: string) => dispatch({ type: 'set_manager_groups_input', value }),
    setOtherUserSubmissionAccess: (value: OtherUserSubmissionAccess) => dispatch({ type: 'set_other_user_submission_access', value }),
  }
}
