import { HardDriveUpload } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  formatProblemTitleDisplay,
  problemDataFilenameValue,
  problemSlugValue,
  shouldShowProblemSlugSupplement,
  useProblemTitleDisplayMode,
  type ProblemSlug,
  type ProblemTitle,
} from '@/features/problem/domain/problem'
import type { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { useI18n } from '@/shared/i18n/i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

export function ProblemDataHeaderCard({ model }: { model: ProblemDataPageModel }) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const titleText = formatProblemTitleDisplay(
    (model.problem?.title ?? 'Problem') as ProblemTitle,
    (model.problem?.slug ?? 'problem') as ProblemSlug,
    problemTitleDisplayMode,
  )

  if (!model.problem) {
    return null
  }

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
      <CardContent className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="rounded-2xl bg-slate-50 px-5 py-4">
            <Label className="text-sm text-slate-500" htmlFor="problem-data-time-limit">
              {t('problem.data.timeLimit')}
            </Label>
            <Input
              id="problem-data-time-limit"
              className="mt-2 bg-white"
              min={1}
              type="number"
              value={model.timeLimitMs}
              onChange={(event) => {
                model.setTimeLimitMs(Number(event.target.value))
              }}
            />
          </div>
          <div className="rounded-2xl bg-slate-50 px-5 py-4">
            <Label className="text-sm text-slate-500" htmlFor="problem-data-space-limit">
              {t('problem.data.spaceLimit')}
            </Label>
            <Input
              id="problem-data-space-limit"
              className="mt-2 bg-white"
              min={1}
              type="number"
              value={model.spaceLimitMb}
              onChange={(event) => {
                model.setSpaceLimitMb(Number(event.target.value))
              }}
            />
          </div>
          <div className="rounded-2xl bg-slate-50 px-5 py-4">
            <p className="text-sm text-slate-500">{t('problem.data.latestFile')}</p>
            <p className="mt-2 text-base font-medium text-slate-900">
              {model.problem.data.value
                ? problemDataFilenameValue(model.problem.data.value)
                : t('problem.data.noDataUploaded')}
            </p>
          </div>
        </div>
        <Button
          type="button"
          className="rounded-2xl bg-slate-950 text-white hover:bg-slate-800"
          disabled={model.isSavingLimits}
          onClick={() => {
            void model.saveLimits()
          }}
        >
          {model.isSavingLimits ? t('problem.detail.savingContent') : t('problem.data.saveLimits')}
        </Button>
      </CardContent>
    </Card>
  )
}
