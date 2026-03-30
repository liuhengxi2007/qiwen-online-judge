import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblem } from '@/features/problem/api/problem-client'
import { validateProblemDraft } from '@/features/problem/domain/problem-form'
import type { ResourceVisibility } from '@/shared/domain/resource-lifecycle'

type CreateProblemPageState = {
  isSubmitting: boolean
  slug: string
  title: string
  statement: string
  visibility: ResourceVisibility
  errorMessage: string
  successMessage: string
}

type CreateProblemPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_visibility'; value: ResourceVisibility }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialState: CreateProblemPageState = {
  isSubmitting: false,
  slug: '',
  title: '',
  statement: '',
  visibility: 'private',
  errorMessage: '',
  successMessage: '',
}

function reducer(state: CreateProblemPageState, action: CreateProblemPageAction): CreateProblemPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, slug: action.value }
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_statement':
      return { ...state, statement: action.value }
    case 'set_visibility':
      return { ...state, visibility: action.value }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return {
        ...state,
        isSubmitting: false,
        slug: '',
        title: '',
        statement: '',
        visibility: 'private',
        errorMessage: '',
        successMessage: 'Problem created successfully.',
      }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useCreateProblemPageModel(canCreate: boolean) {
  const [state, dispatch] = useReducer(reducer, initialState)

  const submit = useCallback(async () => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: 'Problem manager permission required.' })
      return
    }

    const validation = validateProblemDraft({
      slug: state.slug,
      title: state.title,
      statement: state.statement,
      visibility: state.visibility,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return
    }

    dispatch({ type: 'submit_started' })

    try {
      await createProblem(validation.request)
      dispatch({ type: 'submit_succeeded' })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to create problem.'
      dispatch({ type: 'submit_failed', message })
    }
  }, [canCreate, state.slug, state.statement, state.title, state.visibility])

  return {
    ...state,
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setStatement: (value: string) => dispatch({ type: 'set_statement', value }),
    setVisibility: (value: ResourceVisibility) => dispatch({ type: 'set_visibility', value }),
    submit,
  }
}
