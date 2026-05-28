import { useCallback, useState } from 'react'

import { UpdateProblemSet } from '@/apis/problemset/UpdateProblemSet'
import { HttpClientError } from '@/system/api/http-client'
import { validateProblemSetUpdateDraft } from '../functions/problemset-form'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

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
        const updatedProblemSet = await sendAPI(new UpdateProblemSet(problemSetSlug, validation.request))
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
