import { useEffect, useReducer } from 'react'

import type { ProblemDetail } from '@/features/problem/domain/problem'

type ProblemEditorState = {
  title: string
  statement: string
  visibility: 'private' | 'group' | 'public'
}

type ProblemEditorAction =
  | { type: 'hydrate'; problem: ProblemDetail | null }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_visibility'; value: 'private' | 'group' | 'public' }

const initialState: ProblemEditorState = {
  title: '',
  statement: '',
  visibility: 'private',
}

function reducer(state: ProblemEditorState, action: ProblemEditorAction): ProblemEditorState {
  switch (action.type) {
    case 'hydrate':
      return action.problem
        ? {
            title: action.problem.title,
            statement: action.problem.statement,
            visibility: action.problem.visibility,
          }
        : initialState
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_statement':
      return { ...state, statement: action.value }
    case 'set_visibility':
      return { ...state, visibility: action.value }
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
    setVisibility: (value: 'private' | 'group' | 'public') => dispatch({ type: 'set_visibility', value }),
  }
}
