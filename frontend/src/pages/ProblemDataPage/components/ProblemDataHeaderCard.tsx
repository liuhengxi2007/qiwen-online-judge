import { CheckCircle2, HardDriveUpload, PauseCircle } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatProblemTitleDisplay, shouldShowProblemSlugSupplement } from '@/pages/objects/ProblemTitleDisplay'
import { problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { useProblemDataPageModel } from '../hooks/useProblemDataPageModel'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import { useI18n } from '@/system/i18n/use-i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

export function ProblemDataHeaderCard({ model }: { model: ProblemDataPageModel }) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()

  if (!model.problem) {
    return null
  }

  const titleText = formatProblemTitleDisplay(model.problem.title, model.problem.slug, problemTitleDisplayMode)

  return (
    <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
            <HardDriveUpload className="size-5" />
          </div>
          <div>
            <CardTitle className="text-xl text-slate-950">{titleText}</CardTitle>
            {shouldShowProblemSlugSupplement(problemTitleDisplayMode) ? (
              <CardDescription className="mt-2 font-mono text-sm text-slate-500">
                {problemSlugValue(model.problem.slug)}
              </CardDescription>
            ) : null}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex items-center gap-2 text-sm font-medium">
          {model.problem.ready ? (
            <>
              <CheckCircle2 className="size-4 text-emerald-600" />
              <span className="text-emerald-700">{t('problem.data.ready.ready')}</span>
            </>
          ) : (
            <>
              <PauseCircle className="size-4 text-amber-600" />
              <span className="text-amber-700">{t('problem.data.ready.notReady')}</span>
            </>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
