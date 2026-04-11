import { useCallback, useState } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { updateProblem } from '@/features/problem/api/problem-client'
import { validateProblemUpdateDraft } from '@/features/problem/domain/problem-form'
import type { OthersSubmissionAccess, ProblemDetail, ProblemSlug } from '@/features/problem/domain/problem'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'

export function useProblemUpdateAction(problemSlug: ProblemSlug) {
  const [isSaving, setIsSaving] = useState(false)

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
        return { ok: true, problem: updatedProblem, message: 'Problem updated successfully.' }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : 'Unable to update problem.'
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [problemSlug],
  )

  return { isSaving, save }
}
