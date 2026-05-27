import { useCallback, useState } from 'react'

import { HttpClientError } from '@/system/api/http-client'
import { addProblemToProblemSet } from '@/apis/problemset/AddProblemToProblemSet'
import { validateProblemSetLinkDraft } from '../functions/problemset-link-form'
import type { ProblemSetDetail } from '@/objects/problemset/response/ProblemSetDetail'
import type { ProblemSetSlug } from '@/objects/problemset/ProblemSetSlug'
import { useI18n } from '@/system/i18n/use-i18n'

export function useProblemSetLinkProblemAction(problemSetSlug: ProblemSetSlug) {
  const { t } = useI18n()
  const [activeLink, setActiveLink] = useState(false)

  const attachProblem = useCallback(
    async (linkProblemSlug: string): Promise<{ ok: true; problemSet: ProblemSetDetail; message: string } | { ok: false; message: string }> => {
      const validation = validateProblemSetLinkDraft({ problemSlug: linkProblemSlug })
      if (!validation.ok) {
        return { ok: false, message: validation.message }
      }

      setActiveLink(true)
      try {
        const updatedProblemSet = await addProblemToProblemSet(problemSetSlug, validation.request)
        return { ok: true, problemSet: updatedProblemSet, message: t('problemSet.message.linkSuccess') }
      } catch (error) {
        const message = error instanceof HttpClientError ? error.message : t('problemSet.message.linkFailed')
        return { ok: false, message }
      } finally {
        setActiveLink(false)
      }
    },
    [problemSetSlug, t],
  )

  return { activeLink, attachProblem }
}
