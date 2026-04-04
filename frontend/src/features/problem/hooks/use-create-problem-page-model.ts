import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblem } from '@/features/problem/api/problem-client'
import { validateProblemDraft } from '@/features/problem/domain/problem-form'
import { createOwnerOnlyAccessPolicy, type BaseAccess } from '@/shared/domain/resource-lifecycle'

type CreateProblemPageState = {
  isSubmitting: boolean
  slug: string
  title: string
  statement: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  errorMessage: string
  successMessage: string
}

type CreateProblemPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialState: CreateProblemPageState = {
  isSubmitting: false,
  slug: '',
  title: '',
  statement: '',
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
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
    case 'set_base_access':
      return { ...state, baseAccess: action.value }
    case 'set_granted_users_input':
      return { ...state, grantedUsersInput: action.value }
    case 'set_granted_groups_input':
      return { ...state, grantedGroupsInput: action.value }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return {
        ...state,
        isSubmitting: false,
        slug: '',
        title: '',
        statement: '',
        baseAccess: 'owner_only',
        grantedUsersInput: '',
        grantedGroupsInput: '',
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
      baseAccess: state.baseAccess,
      grantedUsersInput: state.grantedUsersInput,
      grantedGroupsInput: state.grantedGroupsInput,
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
  }, [canCreate, state.baseAccess, state.grantedGroupsInput, state.grantedUsersInput, state.slug, state.statement, state.title])

  return {
    ...state,
    accessPolicy: {
      ...createOwnerOnlyAccessPolicy(),
      baseAccess: state.baseAccess,
      viewerGrants: [
        ...splitGrantInput(state.grantedGroupsInput).map((slug) => ({ kind: 'user_group' as const, slug })),
        ...splitGrantInput(state.grantedUsersInput).map((username) => ({ kind: 'user' as const, username })),
      ],
    },
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setStatement: (value: string) => dispatch({ type: 'set_statement', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    submit,
  }
}

function splitGrantInput(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((token) => token.trim())
    .filter((token) => token.length > 0)
}
