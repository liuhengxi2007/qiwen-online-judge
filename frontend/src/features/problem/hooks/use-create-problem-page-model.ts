import { useCallback, useReducer } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { createProblem } from '@/features/problem/http/api/problem-client'
import {
  initialCreateProblemPageState,
  reduceCreateProblemPageState,
} from '@/features/problem/state/create-problem-page-state'
import type { OthersSubmissionAccess } from '@/features/problem/model/OthersSubmissionAccess'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import { validateProblemDraft } from '@/features/problem/lib/problem-form'
import { buildResourceAccessPolicy } from '@/shared/domain/resource-access-input'
import { resourceAccessSubjectParsers } from '@/shared/access/access-subject-parsers'
import { useI18n } from '@/shared/i18n/use-i18n'
import { createOwnerOnlyAccessPolicy, type BaseAccess } from '@/shared/domain/resource-lifecycle'

export function useCreateProblemPageModel(canCreate: boolean) {
  const [state, dispatch] = useReducer(reduceCreateProblemPageState, initialCreateProblemPageState)
  const { t } = useI18n()
  const accessPolicyResult = buildResourceAccessPolicy(
    resourceAccessSubjectParsers,    state.baseAccess,
    state.grantedUsersInput,
    state.grantedGroupsInput,
    state.managerUsersInput,
    state.managerGroupsInput,
  )

  const submit = useCallback(async (): Promise<ProblemDetail | null> => {
    if (!canCreate) {
      dispatch({ type: 'submit_failed', message: t('problem.message.managerPermissionRequired') })
      return null
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
      othersSubmissionAccess: state.othersSubmissionAccess,
    })
    if (!validation.ok) {
      dispatch({ type: 'submit_failed', message: validation.message })
      return null
    }

    dispatch({ type: 'submit_started' })

    try {
      const createdProblem = await createProblem(validation.request)
      dispatch({ type: 'submit_succeeded', message: t('problem.message.createSuccess') })
      return createdProblem
    } catch (error) {
      const message = error instanceof HttpClientError ? error.message : t('problem.message.createFailed')
      dispatch({ type: 'submit_failed', message })
      return null
    }
  }, [canCreate, state.baseAccess, state.grantedGroupsInput, state.grantedUsersInput, state.managerGroupsInput, state.managerUsersInput, state.othersSubmissionAccess, state.slug, state.spaceLimitMb, state.statement, state.timeLimitMs, state.title, t])

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
    setOthersSubmissionAccess: (value: OthersSubmissionAccess) => dispatch({ type: 'set_others_submission_access', value }),
    submit,
  }
}
