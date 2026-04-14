import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblemSet } from '@/features/problemset/api/problemset-client'
import { validateProblemSetDraft } from '@/features/problemset/domain/problemset-form'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'
import { createOwnerOnlyAccessPolicy, type BaseAccess } from '@/shared/domain/resource-lifecycle'
import { useI18n } from '@/shared/i18n/i18n'

type CreateProblemSetPageState = {
  isSubmitting: boolean
  slug: string
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
  errorMessage: string
  successMessage: string
}

type CreateProblemSetPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialState: CreateProblemSetPageState = {
  isSubmitting: false,
  slug: '',
  title: '',
  description: '',
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
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
        description: '',
        baseAccess: 'owner_only',
        grantedUsersInput: '',
        grantedGroupsInput: '',
        errorMessage: '',
        successMessage: 'Problem set created successfully.',
      }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useCreateProblemSetPageModel(canCreate: boolean) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, initialState)
  const accessPolicyResult = buildResourceAccessPolicy(
    state.baseAccess,
    state.grantedUsersInput,
    state.grantedGroupsInput,
  )

  const submit = useCallback(async () => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: t('problemSet.message.managerPermissionRequired') })
      return
    }

    const validation = validateProblemSetDraft({
      slug: state.slug,
      title: state.title,
      description: state.description,
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
      await createProblemSet(validation.request)
      dispatch({ type: 'submit_succeeded' })
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problemSet.message.createFailed')
      dispatch({ type: 'submit_failed', message })
    }
  }, [canCreate, state.baseAccess, state.description, state.grantedGroupsInput, state.grantedUsersInput, state.slug, state.title, t])

  return {
    ...state,
    successMessage: state.successMessage ? t('problemSet.message.createSuccess') : '',
    accessPolicy: accessPolicyResult.ok ? accessPolicyResult.value : createOwnerOnlyAccessPolicy(),
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    submit,
  }
}
