import { Code2 } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { useProblemDetailQuery } from '@/pages/hooks/problem/use-problem-detail-query'
import { formatProblemTitleDisplay, shouldShowProblemSlugSupplement } from '@/objects/problem/problem-display'
import { problemSlugValue } from '@/objects/problem/problem-parsers'
import { useI18n } from '@/system/i18n/use-i18n'
import { useProblemTitleDisplayMode } from '@/pages/hooks/problem/use-problem-title-display'

type ProblemDetailQuery = ReturnType<typeof useProblemDetailQuery>

export function ProblemSubmitHeaderCard({ detailQuery }: { detailQuery: ProblemDetailQuery }) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  if (!detailQuery.problem) {
    return null
  }

  const titleText = formatProblemTitleDisplay(detailQuery.problem.title, detailQuery.problem.slug, problemTitleDisplayMode)

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
            <Code2 className="size-5" />
          </div>
          <div>
            <CardTitle className="text-2xl text-slate-950">{titleText}</CardTitle>
            {shouldShowProblemSlugSupplement(problemTitleDisplayMode) ? (
              <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                {problemSlugValue(detailQuery.problem.slug)}
              </CardDescription>
            ) : null}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm leading-7 text-slate-600">{t('problem.submit.description')}</p>
      </CardContent>
    </Card>
  )
}
