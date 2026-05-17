import { useCallback, useState } from 'react'

import { HttpClientError } from '@/shared/api/http-client'
import { removeProblemFromProblemSet } from '@/features/problemset/api/problemset-client'
import type { ProblemSlug } from '@/features/problem/domain/problem'
import type { ProblemSetDetail, ProblemSetSlug } from '@/features/problemset/domain/problemset'
import { useI18n } from '@/shared/i18n/use-i18n'

export function useProblemSetRemoveProblemAction(problemSetSlug: ProblemSetSlug) {
  const { t } = useI18n()
  const [activeRemovingProblemSlug, setActiveRemovingProblemSlug] = useState<string | null>(null)

  const removeProblem = useCallback(
    async (problemSlug: ProblemSlug): Promise<{ ok: true; problemSet: ProblemSetDetail; message: string } | { ok: false; message: string }> => {
      setActiveRemovingProblemSlug(problemSlug)
      try {
        const updatedProblemSet = await removeProblemFromProblemSet(problemSetSlug, problemSlug)
        return { ok: true, problemSet: updatedProblemSet, message: t('problemSet.message.removeSuccess') }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('problemSet.message.removeFailed')
        return { ok: false, message }
      } finally {
        setActiveRemovingProblemSlug(null)
      }
    },
    [problemSetSlug, t],
  )

  return { activeRemovingProblemSlug, removeProblem }
}
