import { useCallback, useState } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { updateProblemSet } from '@/features/problemset/http/api/problemset-client'
import { validateProblemSetUpdateDraft } from '@/features/problemset/domain/problemset-form'
import type { ProblemSetDetail, ProblemSetSlug } from '@/features/problemset/domain/problemset'
import type { BaseAccess } from '@/shared/domain/resource-lifecycle'
import { useI18n } from '@/shared/i18n/use-i18n'

export function useProblemSetUpdateAction(problemSetSlug: ProblemSetSlug) {
  const { t } = useI18n()
  const [isSaving, setIsSaving] = useState(false)

  const save = useCallback(
    async (draft: {
      title: string
      description: string
      baseAccess: BaseAccess
      grantedUsersInput: string
      grantedGroupsInput: string
    }): Promise<{ ok: true; problemSet: ProblemSetDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateProblemSetUpdateDraft(draft)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsSaving(true)
      try {
        const updatedProblemSet = await updateProblemSet(problemSetSlug, validation.request)
        return { ok: true, problemSet: updatedProblemSet, message: t('problemSet.message.updateSuccess') }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('problemSet.message.updateFailed')
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [problemSetSlug, t],
  )

  return { isSaving, save }
}
