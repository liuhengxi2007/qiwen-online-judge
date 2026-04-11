import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblem } from '@/features/problem/api/problem-client'
import { validateProblemDraft } from '@/features/problem/domain/problem-form'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'
import { createOwnerOnlyAccessPolicy, type BaseAccess } from '@/shared/domain/resource-lifecycle'

type CreateProblemPageState = {
  isSubmitting: boolean
  slug: string
  title: string
  statement: string
  timeLimitMs: number
  spaceLimitMb: number
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  managerUsersInput: string
  managerGroupsInput: string
  errorMessage: string
  successMessage: string
}

type CreateProblemPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_statement'; value: string }
  | { type: 'set_time_limit_ms'; value: number }
  | { type: 'set_space_limit_mb'; value: number }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'set_manager_users_input'; value: string }
  | { type: 'set_manager_groups_input'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialState: CreateProblemPageState = {
  isSubmitting: false,
  slug: '',
  title: '',
  statement: '',
  timeLimitMs: 1000,
  spaceLimitMb: 256,
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
  managerUsersInput: '',
  managerGroupsInput: '',
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
    case 'set_time_limit_ms':
      return { ...state, timeLimitMs: action.value }
    case 'set_space_limit_mb':
      return { ...state, spaceLimitMb: action.value }
    case 'set_base_access':
      return { ...state, baseAccess: action.value }
    case 'set_granted_users_input':
      return { ...state, grantedUsersInput: action.value }
    case 'set_granted_groups_input':
      return { ...state, grantedGroupsInput: action.value }
    case 'set_manager_users_input':
      return { ...state, managerUsersInput: action.value }
    case 'set_manager_groups_input':
      return { ...state, managerGroupsInput: action.value }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return {
        ...state,
        isSubmitting: false,
        slug: '',
        title: '',
        statement: '',
        timeLimitMs: 1000,
        spaceLimitMb: 256,
        baseAccess: 'owner_only',
        grantedUsersInput: '',
        grantedGroupsInput: '',
        managerUsersInput: '',
        managerGroupsInput: '',
        errorMessage: '',
        successMessage: 'Problem created successfully.',
      }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useCreateProblemPageModel(canCreate: boolean) {
  const [state, dispatch] = useReducer(reducer, initialState)
  const accessPolicyResult = buildResourceAccessPolicy(
    state.baseAccess,
    state.grantedUsersInput,
    state.grantedGroupsInput,
    state.managerUsersInput,
    state.managerGroupsInput,
  )

  const submit = useCallback(async () => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: 'Problem manager permission required.' })
      return
    }

    const validation = validateProblemDraft({
      slug: state.slug,
      title: state.title,
      statement: state.statement,
      timeLimitMs: state.timeLimitMs,
      spaceLimitMb: state.spaceLimitMb,
      baseAccess: state.baseAccess,
      grantedUsersInput: state.grantedUsersInput,
      grantedGroupsInput: state.grantedGroupsInput,
      managerUsersInput: state.managerUsersInput,
      managerGroupsInput: state.managerGroupsInput,
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
  }, [canCreate, state.baseAccess, state.grantedGroupsInput, state.grantedUsersInput, state.managerGroupsInput, state.managerUsersInput, state.slug, state.spaceLimitMb, state.statement, state.timeLimitMs, state.title])

  return {
    ...state,
    accessPolicy: accessPolicyResult.ok ? accessPolicyResult.value : createOwnerOnlyAccessPolicy(),
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setStatement: (value: string) => dispatch({ type: 'set_statement', value }),
    setTimeLimitMs: (value: number) => dispatch({ type: 'set_time_limit_ms', value }),
    setSpaceLimitMb: (value: number) => dispatch({ type: 'set_space_limit_mb', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    setManagerUsersInput: (value: string) => dispatch({ type: 'set_manager_users_input', value }),
    setManagerGroupsInput: (value: string) => dispatch({ type: 'set_manager_groups_input', value }),
    submit,
  }
}
