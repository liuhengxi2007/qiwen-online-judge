import { useEffect, useReducer, useState } from 'react'

import type { ProblemSetDetail } from '@/features/problemset/domain/problemset'

type ProblemSetEditorState = {
  title: string
  description: string
  visibility: 'private' | 'group' | 'public'
}

type ProblemSetEditorAction =
  | { type: 'hydrate'; problemSet: ProblemSetDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_visibility'; value: 'private' | 'group' | 'public' }

const initialState: ProblemSetEditorState = {
  title: '',
  description: '',
  visibility: 'private',
}

function reducer(state: ProblemSetEditorState, action: ProblemSetEditorAction): ProblemSetEditorState {
  switch (action.type) {
    case 'hydrate':
      return action.problemSet
        ? {
            title: action.problemSet.title,
            description: action.problemSet.description,
            visibility: action.problemSet.visibility,
          }
        : initialState
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_description':
      return { ...state, description: action.value }
    case 'set_visibility':
      return { ...state, visibility: action.value }
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
    setVisibility: (value: 'private' | 'group' | 'public') => dispatch({ type: 'set_visibility', value }),
    setLinkProblemSlug,
    clearLinkedProblemSlug: () => setLinkProblemSlug(''),
  }
}
