import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { displayNameValue, usernameValue } from '@/features/auth/domain/auth'
import { ProblemDataFilesCard } from '@/features/problem/components/problem-data-files-card'
import { ProblemDataHeaderCard } from '@/features/problem/components/problem-data-header-card'
import { ProblemDataUploadCard } from '@/features/problem/components/problem-data-upload-card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { parseProblemSlug, problemSlugValue } from '@/features/problem/domain/problem'
import { useProblemDataPageModel } from '@/features/problem/hooks/use-problem-data-page-model'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

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

  const model = useProblemDataPageModel(slugResult.value)

  if (!model.isProblemLoading && model.problem && !model.problem.canManage) {
    return <Navigate replace to={`/problems/${problemSlugValue(slugResult.value)}`} />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf5f1_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">{t('problem.data.heading')}</h1>
            <p className="text-sm text-slate-600">
              {t('common.signedInAs', { displayName: displayNameValue(user.displayName), username: usernameValue(user.username) })}
            </p>
          </div>

          <AncestorNavigation />
        </div>

        {model.isProblemLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('problem.data.loading')}</CardContent>
          </Card>
        ) : model.problem ? (
          <div className="space-y-6">
            <ProblemDataHeaderCard model={model} />
            <ProblemDataUploadCard model={model} />
            <ProblemDataFilesCard model={model} problemSlug={slugResult.value} />
          </div>
        ) : (
          <Alert variant="destructive" className="rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">
              {model.problemErrorMessage || t('problem.data.loadFailed')}
            </AlertDescription>
          </Alert>
        )}
      </section>
    </main>
  )
}
