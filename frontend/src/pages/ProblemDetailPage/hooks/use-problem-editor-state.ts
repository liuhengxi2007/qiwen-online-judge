import { useEffect, useReducer } from 'react'

import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import {
  initialProblemEditorState,
  reduceProblemEditorState,
} from '../functions/problem-editor-state'
import type { BaseAccess } from '@/objects/shared/resource-lifecycle'

export function useProblemEditorState(problem: ProblemDetail | null) {
  const [state, dispatch] = useReducer(reduceProblemEditorState, initialProblemEditorState)

  useEffect(() => {
    dispatch({ type: 'hydrate', problem })
  }, [problem])

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
