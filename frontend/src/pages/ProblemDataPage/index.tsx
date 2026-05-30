import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { ProblemDataFilesCard } from './components/ProblemDataFilesCard'
import { ProblemDataHeaderCard } from './components/ProblemDataHeaderCard'
import { ProblemDataUploadCard } from './components/ProblemDataUploadCard'
import { ProblemJudgeConfigEditorCard } from './components/ProblemJudgeConfigEditorCard'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseProblemSlug, problemSlugValue } from '@/objects/problem/ProblemSlug'
import type { ProblemSlug } from '@/objects/problem/ProblemSlug'
import { useProblemDataPageModel } from './hooks/useProblemDataPageModel'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

export function ProblemDataPage() {
  const { t } = useI18n()
  usePageTitle(t('problem.data.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { slug } = useParams<{ slug: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const slugResult = parseProblemSlug(slug ?? '')
  if (!slugResult.ok) {
    return <Navigate replace to="/problems" />
  }

  return <ProblemDataPageContent problemSlug={slugResult.value} />
}

function ProblemDataPageContent({ problemSlug }: { problemSlug: ProblemSlug }) {
  const { t } = useI18n()
  const model = useProblemDataPageModel(problemSlug)

  if (!model.isProblemLoading && model.problem && !model.problem.canManage) {
    return <Navigate replace to={`/problems/${problemSlugValue(problemSlug)}`} />
  }

  return (
    <PageShell title={t('problem.data.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)]">
      {model.isProblemLoading ? (
        <PageLoadingCard message={t('problem.data.loading')} />
      ) : model.problem ? (
        <div className="space-y-6">
          <ProblemDataHeaderCard model={model} />
          <ProblemDataUploadCard model={model} />
          <ProblemJudgeConfigEditorCard model={model} problemSlug={problemSlug} />
          <ProblemDataFilesCard model={model} />
        </div>
      ) : (
        <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
          <AlertDescription className="text-rose-700">
            {model.problemErrorMessage || t('problem.data.loadFailed')}
          </AlertDescription>
        </Alert>
      )}
    </PageShell>
  )
}
