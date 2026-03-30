import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblemSet } from '@/features/problemset/api/problemset-client'
import { validateProblemSetDraft } from '@/features/problemset/domain/problemset-form'
import type { ResourceVisibility } from '@/shared/domain/resource-lifecycle'

type CreateProblemSetPageState = {
  isSubmitting: boolean
  slug: string
  title: string
  description: string
  visibility: ResourceVisibility
  errorMessage: string
  successMessage: string
}

type CreateProblemSetPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_visibility'; value: ResourceVisibility }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialState: CreateProblemSetPageState = {
  isSubmitting: false,
  slug: '',
  title: '',
  description: '',
  visibility: 'private',
  errorMessage: '',
  successMessage: '',
}

function reducer(state: CreateProblemSetPageState, action: CreateProblemSetPageAction): CreateProblemSetPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, slug: action.value }
    case 'set_title':
      return { ...state, title: action.value }
    case 'set_description':
      return { ...state, description: action.value }
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
        description: '',
        visibility: 'private',
        errorMessage: '',
        successMessage: 'Problem set created successfully.',
      }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useCreateProblemSetPageModel(canCreate: boolean) {
  const [state, dispatch] = useReducer(reducer, initialState)

  const submit = useCallback(async () => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: 'Problem manager permission required.' })
      return
    }

    const validation = validateProblemSetDraft({
      slug: state.slug,
      title: state.title,
      description: state.description,
      visibility: state.visibility,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return
    }

    dispatch({ type: 'submit_started' })

    try {
      await createProblemSet(validation.request)
      dispatch({ type: 'submit_succeeded' })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to create problem set.'
      dispatch({ type: 'submit_failed', message })
    }
  }, [canCreate, state.description, state.slug, state.title, state.visibility])

  return {
    ...state,
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setVisibility: (value: ResourceVisibility) => dispatch({ type: 'set_visibility', value }),
    submit,
  }
}
