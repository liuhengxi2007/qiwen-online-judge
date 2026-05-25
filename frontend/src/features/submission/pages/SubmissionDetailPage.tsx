import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { parseSubmissionId } from '@/features/submission/lib/submission-parsers'
import type { SubmissionId } from '@/features/submission/model/SubmissionId'
import { useProblemTitleDisplayMode } from '@/features/problem/hooks/use-problem-title-display'
import { useSubmissionDetailActions } from '@/features/submission/hooks/use-submission-detail-actions'
import { useSubmissionDetailQuery } from '@/features/submission/hooks/use-submission-detail-query'
import { AppSectionBar } from '@/features/auth/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'

import { SubmissionJudgeMessageCard } from './SubmissionDetailPage/components/SubmissionJudgeMessageCard'
import { SubmissionJudgeResultCard } from './SubmissionDetailPage/components/SubmissionJudgeResultCard'
import { SubmissionSourceCodeCard } from './SubmissionDetailPage/components/SubmissionSourceCodeCard'
import { SubmissionSummaryCard } from './SubmissionDetailPage/components/SubmissionSummaryCard'

export function SubmissionDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('submission.detail.pageTitle'))
  const { session: user, navigationIntent } = useSessionGuard()
  const { submissionId } = useParams<{ submissionId: string }>()

  if (navigationIntent) {
    return <Navigate replace={navigationIntent.replace} to={navigationIntent.to} />
  }

  if (!user) {
    return <Navigate replace to="/login" />
  }

  const parsedSubmissionId = Number(submissionId ?? '')
  const submissionIdResult = parseSubmissionId(parsedSubmissionId)
  if (!submissionIdResult.ok) {
    return <Navigate replace to="/submissions" />
  }

  return <SubmissionDetailPageContent currentSubmissionId={submissionIdResult.value} />
}

function SubmissionDetailPageContent({ currentSubmissionId }: { currentSubmissionId: SubmissionId }) {
  const { t } = useI18n()
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
  const submissionQuery = useSubmissionDetailQuery(currentSubmissionId)
  const submissionActions = useSubmissionDetailActions({
    submissionId: currentSubmissionId,
    replaceSubmission: submissionQuery.replaceSubmission,
    rejudgeFailedMessage: t('submission.detail.rejudgeFailed'),
    deleteFailedMessage: t('submission.detail.deleteFailed'),
  })

  if (submissionActions.deleted) {
    return <Navigate replace to="/submissions" />
  }

  if (!submissionQuery.isLoading && !submissionQuery.submission) {
    return <Navigate replace to="/submissions" />
  }

  return (
    <main className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)] px-6 py-12 sm:px-8">
      <section className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-2">
            <p className="text-sm uppercase tracking-[0.25em] text-slate-500">{t('common.siteName')}</p>
            <h1 className="font-['Georgia'] text-4xl font-semibold tracking-tight text-slate-950">
              {t('submission.detail.heading')}
            </h1>
          </div>

          <AncestorNavigation />
        </div>

        <AppSectionBar />

        {submissionQuery.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{submissionQuery.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {submissionActions.actionErrorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{submissionActions.actionErrorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {submissionQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('submission.detail.loading')}</CardContent>
          </Card>
        ) : submissionQuery.submission ? (
          <div className="space-y-6">
            <SubmissionSummaryCard
              deleteCurrentSubmission={submissionActions.deleteCurrentSubmission}
              isDeleting={submissionActions.isDeleting}
              isRejudging={submissionActions.isRejudging}
              problemTitleDisplayMode={problemTitleDisplayMode}
              rejudge={submissionActions.rejudge}
              submission={submissionQuery.submission}
            />

            {submissionQuery.submission.judgeResult ? (
              <SubmissionJudgeResultCard judgeResult={submissionQuery.submission.judgeResult} />
            ) : null}

            {submissionQuery.submission.judgeMessage ? (
              <SubmissionJudgeMessageCard judgeMessage={submissionQuery.submission.judgeMessage} />
            ) : null}

            <SubmissionSourceCodeCard sourceCode={submissionQuery.submission.sourceCode} />
          </div>
        ) : null}
      </section>
    </main>
  )
}
