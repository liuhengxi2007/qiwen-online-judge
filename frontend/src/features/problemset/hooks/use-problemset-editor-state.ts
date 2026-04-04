import { useEffect, useReducer, useState } from 'react'

import type { ProblemSetDetail } from '@/features/problemset/domain/problemset'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

type ProblemSetEditorState = {
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
}

type ProblemSetEditorAction =
  | { type: 'hydrate'; problemSet: ProblemSetDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }

const initialState: ProblemSetEditorState = {
  title: '',
  description: '',
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
}

function reducer(state: ProblemSetEditorState, action: ProblemSetEditorAction): ProblemSetEditorState {
  switch (action.type) {
    case 'hydrate':
      return action.problemSet
        ? {
            title: action.problemSet.title,
            description: action.problemSet.description,
            baseAccess: action.problemSet.accessPolicy.baseAccess,
            grantedUsersInput: action.problemSet.accessPolicy.viewerGrants
              .filter((grant) => grant.kind === 'user')
              .map((grant) => grant.username)
              .join('\n'),
            grantedGroupsInput: action.problemSet.accessPolicy.viewerGrants
              .filter((grant) => grant.kind === 'user_group')
              .map((grant) => grant.slug)
              .join('\n'),
          }
        : initialState
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
  }
}

export function useProblemSetEditorState(problemSet: ProblemSetDetail | null) {
  const [editorState, dispatch] = useReducer(reducer, initialState)
  const [linkProblemSlug, setLinkProblemSlug] = useState('')

  useEffect(() => {
    dispatch({ type: 'hydrate', problemSet })
  }, [problemSet])

  return {
    ...editorState,
    linkProblemSlug,
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    setLinkProblemSlug,
    clearLinkedProblemSlug: () => setLinkProblemSlug(''),
  }
}
