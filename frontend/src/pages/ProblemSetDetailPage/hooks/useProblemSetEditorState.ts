import { useEffect, useReducer } from 'react'

import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import {
  initialProblemSetEditorState,
  reduceProblemSetEditorState,
} from '../functions/ProblemSetEditorState'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'

export function useProblemSetEditorState(problemSet: ProblemSetDetail | null) {
  const [editorState, dispatch] = useReducer(reduceProblemSetEditorState, initialProblemSetEditorState)

  useEffect(() => {
    dispatch({ type: 'hydrate', problemSet })
  }, [problemSet])

  return {
    ...editorState,
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    setLinkProblemSlug: (value: string) => dispatch({ type: 'set_link_problem_slug', value }),
    clearLinkedProblemSlug: () => dispatch({ type: 'clear_link_problem_slug' }),
  }
}
