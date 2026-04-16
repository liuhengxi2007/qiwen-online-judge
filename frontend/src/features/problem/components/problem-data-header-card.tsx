import { HardDriveUpload } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { problemDataFilenameValue, problemSlugValue, problemTitleValue } from '@/features/problem/domain/problem'
import type { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { useI18n } from '@/shared/i18n/i18n'

type ProblemDataPageModel = ReturnType<typeof useProblemDataPageModel>

export function ProblemDataHeaderCard({ model }: { model: ProblemDataPageModel }) {
  const { t } = useI18n()

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
            <CardTitle className="text-xl text-slate-950">{problemTitleValue(model.problem.title)}</CardTitle>
            <CardDescription className="mt-2 font-mono text-sm text-slate-500">
              {problemSlugValue(model.problem.slug)}
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="rounded-2xl bg-slate-50 px-5 py-4">
            <p className="text-sm text-slate-500">{t('problem.data.timeLimit')}</p>
            <p className="mt-2 text-lg font-semibold text-slate-900">{model.problem.timeLimitMs} ms</p>
          </div>
          <div className="rounded-2xl bg-slate-50 px-5 py-4">
            <p className="text-sm text-slate-500">{t('problem.data.spaceLimit')}</p>
            <p className="mt-2 text-lg font-semibold text-slate-900">{model.problem.spaceLimitMb} MB</p>
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
      </CardContent>
    </Card>
  )
}
