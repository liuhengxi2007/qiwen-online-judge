import { useCallback, useState } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { updateProblem } from '@/features/problem/http/api/UpdateProblem'
import { validateProblemUpdateDraft } from '@/features/problem/lib/problem-form'
import type { OthersSubmissionAccess } from '@/features/problem/model/OthersSubmissionAccess'
import type { ProblemDetail } from '@/features/problem/http/response/ProblemDetail'
import type { ProblemSlug } from '@/features/problem/model/ProblemSlug'
import { useI18n } from '@/shared/i18n/use-i18n'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

export function useProblemUpdateAction(problemSlug: ProblemSlug) {
  const [isSaving, setIsSaving] = useState(false)
  const { t } = useI18n()

  const save = useCallback(
    async (draft: {
      title: string
      statement: string
      timeLimitMs: number
      spaceLimitMb: number
      baseAccess: BaseAccess
      grantedUsersInput: string
      grantedGroupsInput: string
      managerUsersInput: string
      managerGroupsInput: string
      othersSubmissionAccess: OthersSubmissionAccess
    }): Promise<{ ok: true; problem: ProblemDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateProblemUpdateDraft(draft)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsSaving(true)
      try {
        const updatedProblem = await updateProblem(problemSlug, validation.request)
        return { ok: true, problem: updatedProblem, message: t('problem.message.updateSuccess') }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('problem.message.updateFailed')
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [problemSlug, t],
  )

  return { isSaving, save }
}
