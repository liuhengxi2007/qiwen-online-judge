import { useCallback, useState } from 'react'

import { UpdateProblem } from '@/apis/problem/UpdateProblem'
import { isHttpClientError } from '@/system/api/http-client'
import { validateProblemUpdateDraft } from '../functions/ProblemForm'
import type { ContestSlug } from '@/objects/contest/ContestSlug'
import type { OtherUserSubmissionAccess } from '@/objects/problem/OtherUserSubmissionAccess'
import type { ProblemDetail } from '@/objects/problem/response/ProblemDetail'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useI18n } from '@/system/i18n/use-i18n'
import type { BaseAccess } from '@/objects/shared/access/BaseAccess'
import { sendAPI } from '@/system/api/api-message'

/**
 * 更新题目动作 hook；按普通或比赛上下文提交题目更新请求。
 */
export function useProblemUpdateAction(problemSlug: ProblemSlug, contestSlug?: ContestSlug) {
  const [isSaving, setIsSaving] = useState(false)
  const { t } = useI18n()

  const save = useCallback(
    async (draft: {
      title: string
      statement: string
      authorUsername: string
      baseAccess: BaseAccess
      grantedUsersInput: string
      grantedGroupsInput: string
      managerUsersInput: string
      managerGroupsInput: string
      otherUserSubmissionAccess: OtherUserSubmissionAccess
    }): Promise<{ ok: true; problem: ProblemDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateProblemUpdateDraft(draft)
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setIsSaving(true)
      try {
        const updatedProblem = await sendAPI(new UpdateProblem(problemSlug, validation.request, contestSlug))
        return { ok: true, problem: updatedProblem, message: t('problem.message.updateSuccess') }
      } catch (error) {
        const message = isHttpClientError(error) ? error.message : t('problem.message.updateFailed')
        return { ok: false, message }
      } finally {
        setIsSaving(false)
      }
    },
    [contestSlug, problemSlug, t],
  )

  return { isSaving, save }
}
