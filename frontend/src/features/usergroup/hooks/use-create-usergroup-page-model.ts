import { useCallback, useReducer } from 'react'

import { createUserGroup } from '@/features/usergroup/api/usergroup-client'
import { validateUserGroupDraft } from '@/features/usergroup/domain/usergroup-form'
import { HttpClientError } from '@/shared/api/http-client'

type CreateUserGroupPageState = {
  isSubmitting: boolean
  slug: string
  name: string
  description: string
  errorMessage: string
  successMessage: string
}

type CreateUserGroupPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_name'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialState: CreateUserGroupPageState = {
  isSubmitting: false,
  slug: '',
  name: '',
  description: '',
  errorMessage: '',
  successMessage: '',
}

function reducer(state: CreateUserGroupPageState, action: CreateUserGroupPageAction): CreateUserGroupPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, slug: action.value }
    case 'set_name':
      return { ...state, name: action.value }
    case 'set_description':
      return { ...state, description: action.value }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return {
        ...state,
        isSubmitting: false,
        slug: '',
        name: '',
        description: '',
        errorMessage: '',
        successMessage: 'User group created successfully.',
      }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useCreateUserGroupPageModel() {
  const [state, dispatch] = useReducer(reducer, initialState)

  const submit = useCallback(async () => {
    const validation = validateUserGroupDraft({
      slug: state.slug,
      name: state.name,
      description: state.description,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return
    }

    dispatch({ type: 'submit_started' })

    try {
      await createUserGroup(validation.request)
      dispatch({ type: 'submit_succeeded' })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : 'Unable to create user group.'
      dispatch({ type: 'submit_failed', message })
    }
  }, [state.description, state.name, state.slug])

  return {
    ...state,
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setName: (value: string) => dispatch({ type: 'set_name', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    submit,
  }
}
