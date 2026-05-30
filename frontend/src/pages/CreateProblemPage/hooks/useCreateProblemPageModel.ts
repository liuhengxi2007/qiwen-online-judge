import { useCallback, useReducer } from 'react'

import { CreateProblem } from '@/apis/problem/CreateProblem'
import { HttpClientError } from '@/system/api/http-client'
import {
  initialCreateProblemPageState,
  reduceCreateProblemPageState,
} from '../functions/CreateProblemPageState'
import type { OthersSubmissionAccess } from '@/objects/problem/OthersSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import { validateProblemDraft } from '../functions/ProblemForm'
import { buildResourceAccessPolicy } from '@/pages/components/ResourceAccessEditorInput'
import { useI18n } from '@/system/i18n/use-i18n'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { createOwnerOnlyAccessPolicy } from '@/objects/shared/access/ResourceAccessPolicy'
import { sendAPI } from '@/system/api/api-message'

export function useCreateProblemPageModel(canCreate: boolean) {
  const [state, dispatch] = useReducer(reduceCreateProblemPageState, initialCreateProblemPageState)
  const { t } = useI18n()
  const accessPolicyResult = buildResourceAccessPolicy(
    state.draft.baseAccess,
    state.draft.grantedUsersInput,
    state.draft.grantedGroupsInput,
    state.draft.managerUsersInput,
    state.draft.managerGroupsInput,
  )

  const submit = useCallback(async (): Promise<ProblemDetail | null> => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: t('problem.message.managerPermissionRequired') })
      return null
    }

    const validation = validateProblemDraft(state.draft)
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return null
    }

    dispatch({ type: 'submit_started' })

    try {
      const createdProblem = await sendAPI(new CreateProblem(validation.request))
      dispatch({ type: 'submit_succeeded', message: t('problem.message.createSuccess') })
      return createdProblem
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problem.message.createFailed')
      dispatch({ type: 'submit_failed', message })
      return null
    }
  }, [canCreate, state.draft, t])

  return {
    ...state.draft,
    isSubmitting: state.isSubmitting,
    errorMessage: state.errorMessage,
    successMessage: state.successMessage,
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
    setOthersSubmissionAccess: (value: OthersSubmissionAccess) => dispatch({ type: 'set_others_submission_access', value }),
    submit,
  }
}
