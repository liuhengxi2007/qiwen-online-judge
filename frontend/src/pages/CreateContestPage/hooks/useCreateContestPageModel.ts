import { useCallback, useReducer } from 'react'

import { CreateContest } from '@/apis/contest/CreateContest'
import type { ContestDetail } from '@/objects/contest/response/ContestDetail'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { createRestrictedAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'
import { sendAPI } from '@/system/api/api-message'
import { HttpClientError } from '@/system/api/http-client'
import { useI18n } from '@/system/i18n/use-i18n'
import type { CreateContestDraft } from '../functions/ContestForm'
import { validateContestDraft } from '../functions/ContestForm'

type CreateContestPageState = {
  isSubmitting: boolean
  draft: CreateContestDraft
  errorMessage: string
  successMessage: string
}

type CreateContestPageAction =
  | { type: 'set_slug'; value: string }
  | { type: 'set_title'; value: string }
  | { type: 'set_description'; value: string }
  | { type: 'set_start_at'; value: string }
  | { type: 'set_end_at'; value: string }
  | { type: 'set_base_access'; value: BaseAccess }
  | { type: 'set_granted_users_input'; value: string }
  | { type: 'set_granted_groups_input'; value: string }
  | { type: 'submit_started' }
  | { type: 'submit_succeeded' }
  | { type: 'submit_failed'; message: string }

const initialDraft: CreateContestDraft = {
  slug: '',
  title: '',
  description: '',
  startAt: '',
  endAt: '',
  baseAccess: 'restricted',
  grantedUsersInput: '',
  grantedGroupsInput: '',
}

const initialState: CreateContestPageState = {
  isSubmitting: false,
  draft: initialDraft,
  errorMessage: '',
  successMessage: '',
}

function reducer(state: CreateContestPageState, action: CreateContestPageAction): CreateContestPageState {
  switch (action.type) {
    case 'set_slug':
      return { ...state, draft: { ...state.draft, slug: action.value } }
    case 'set_title':
      return { ...state, draft: { ...state.draft, title: action.value } }
    case 'set_description':
      return { ...state, draft: { ...state.draft, description: action.value } }
    case 'set_start_at':
      return { ...state, draft: { ...state.draft, startAt: action.value } }
    case 'set_end_at':
      return { ...state, draft: { ...state.draft, endAt: action.value } }
    case 'set_base_access':
      return { ...state, draft: { ...state.draft, baseAccess: action.value } }
    case 'set_granted_users_input':
      return { ...state, draft: { ...state.draft, grantedUsersInput: action.value } }
    case 'set_granted_groups_input':
      return { ...state, draft: { ...state.draft, grantedGroupsInput: action.value } }
    case 'submit_started':
      return { ...state, isSubmitting: true, errorMessage: '', successMessage: '' }
    case 'submit_succeeded':
      return { ...state, isSubmitting: false, draft: initialDraft, errorMessage: '', successMessage: 'Contest created successfully.' }
    case 'submit_failed':
      return { ...state, isSubmitting: false, errorMessage: action.message, successMessage: '' }
  }
}

export function useCreateContestPageModel(canCreate: boolean) {
  const { t } = useI18n()
  const [state, dispatch] = useReducer(reducer, initialState)
  const accessPolicyResult = buildResourceAccessPolicy(
    state.draft.baseAccess,
    state.draft.grantedUsersInput,
    state.draft.grantedGroupsInput,
  )

  const submit = useCallback(async (): Promise<ContestDetail | null> => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: t('contest.create.permissionRequired') })
      return null
    }

    const validation = validateContestDraft(state.draft)
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return null
    }

    dispatch({ type: 'submit_started' })

    try {
      const createdContest = await sendAPI(new CreateContest(validation.request))
      dispatch({ type: 'submit_succeeded' })
      return createdContest
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('contest.create.failed')
      dispatch({ type: 'submit_failed', message })
      return null
    }
  }, [canCreate, state.draft, t])

  return {
    ...state.draft,
    isSubmitting: state.isSubmitting,
    errorMessage: state.errorMessage,
    successMessage: state.successMessage ? t('contest.create.success') : '',
    accessPolicy: accessPolicyResult.ok ? accessPolicyResult.value : createRestrictedAccessPolicy(),
    setSlug: (value: string) => dispatch({ type: 'set_slug', value }),
    setTitle: (value: string) => dispatch({ type: 'set_title', value }),
    setDescription: (value: string) => dispatch({ type: 'set_description', value }),
    setStartAt: (value: string) => dispatch({ type: 'set_start_at', value }),
    setEndAt: (value: string) => dispatch({ type: 'set_end_at', value }),
    setBaseAccess: (value: BaseAccess) => dispatch({ type: 'set_base_access', value }),
    setGrantedUsersInput: (value: string) => dispatch({ type: 'set_granted_users_input', value }),
    setGrantedGroupsInput: (value: string) => dispatch({ type: 'set_granted_groups_input', value }),
    submit,
  }
}
