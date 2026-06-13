import { useCallback, useState } from 'react'

import { RemoveProblemFromProblemSet } from '@/apis/problemset/RemoveProblemFromProblemSet'
import { isHttpClientError } from '@/system/api/http-client'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { sendAPI } from '@/system/api/api-message'
import { useI18n } from '@/system/i18n/use-i18n'

/**
 * 题单移除题目动作 hook；提交移除请求并返回操作状态。
 */
export function useProblemSetRemoveProblemAction(problemSetSlug: ProblemSetSlug) {
  const { t } = useI18n()
  const [activeRemovingProblemSlug, setActiveRemovingProblemSlug] = useState<string | null>(null)

  const removeProblem = useCallback(
    async (problemSlug: ProblemSlug): Promise<{ ok: true; problemSet: ProblemSetDetail; message: string } | { ok: false; message: string }> => {
      setActiveRemovingProblemSlug(problemSlug)
      try {
        const updatedProblemSet = await sendAPI(new RemoveProblemFromProblemSet(problemSetSlug, problemSlug))
        return { ok: true, problemSet: updatedProblemSet, message: t('problemSet.message.removeSuccess') }
      } catch (error) {
        const message = isHttpClientError(error) ? error.message : t('problemSet.message.removeFailed')
        return { ok: false, message }
      } finally {
        setActiveRemovingProblemSlug(null)
      }
    },
    [problemSetSlug, t],
  )

  return { activeRemovingProblemSlug, removeProblem }
}
