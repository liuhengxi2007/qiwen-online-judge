import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblemSet } from '@/features/problemset/http/api/CreateProblemSet'
import type { ProblemSetSummary } from '@/features/problemset/model/response/ProblemSetSummary'
import { validateProblemSetDraft } from '@/features/problemset/lib/problemset-form'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'
import { resourceAccessSubjectParsers } from '@/shared/domain/access/access-subject-parsers'
import { createOwnerOnlyAccessPolicy, type BaseAccess } from '@/shared/domain/resource-lifecycle'
import { useI18n } from '@/shared/i18n/use-i18n'

type CreateProblemSetPageState = {
  isSubmitting: boolean
  draft: CreateProblemSetDraft
  errorMessage: string
  successMessage: string
}

type CreateProblemSetDraft = {
  slug: string
  title: string
  description: string
  baseAccess: BaseAccess
  grantedUsersInput: string
  grantedGroupsInput: string
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

const initialDraft: CreateProblemSetDraft = {
  slug: '',
  title: '',
  description: '',
  baseAccess: 'owner_only',
  grantedUsersInput: '',
  grantedGroupsInput: '',
}

const initialState: CreateProblemSetPageState = {
  isSubmitting: false,
  draft: initialDraft,
  errorMessage: '',
  successMessage: '',
}

function reducer(state: CreateProblemSetPageState, action: CreateProblemSetPageAction): CreateProblemSetPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, draft: { ...state.draft, slug: action.value } }
    case 'set_title':
      return { ...state, draft: { ...state.draft, title: action.value } }
    case 'set_description':
      return { ...state, draft: { ...state.draft, description: action.value } }
    case 'set_base_access':
      return { ...state, draft: { ...state.draft, baseAccess: action.value } }
    case 'set_granted_users_input':
      return { ...state, draft: { ...state.draft, grantedUsersInput: action.value } }
    case 'set_granted_groups_input':
      return { ...state, draft: { ...state.draft, grantedGroupsInput: action.value } }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return {
        ...state,
        isSubmitting: false,
        draft: initialDraft,
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
    resourceAccessSubjectParsers,
    state.draft.baseAccess,
    state.draft.grantedUsersInput,
    state.draft.grantedGroupsInput,
  )

  const submit = useCallback(async (): Promise<ProblemSetSummary | null> => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: t('problemSet.message.managerPermissionRequired') })
      return null
    }

    const validation = validateProblemSetDraft(state.draft)
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return null
    }

    dispatch({ type: 'submit_started' })

    try {
      const createdProblemSet = await createProblemSet(validation.request)
      dispatch({ type: 'submit_succeeded' })
      return createdProblemSet
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problemSet.message.createFailed')
      dispatch({ type: 'submit_failed', message })
      return null
    }
  }, [canCreate, state.draft, t])

  return {
    ...state.draft,
    isSubmitting: state.isSubmitting,
    errorMessage: state.errorMessage,
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
