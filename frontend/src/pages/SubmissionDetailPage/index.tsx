import { Navigate, useParams } from 'react-router-dom'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { useSessionGuard } from '@/pages/hooks/useSessionGuard'
import { parseSubmissionId } from '@/objects/submission/SubmissionId'
import type { SubmissionId } from '@/objects/submission/SubmissionId'
import { useProblemTitleDisplayMode } from '@/pages/hooks/useProblemTitleDisplay'
import { useSubmissionDetailActions } from './hooks/useSubmissionDetailActions'
import { useSubmissionDetailQuery } from './hooks/useSubmissionDetailQuery'
import { PageLoadingCard } from '@/pages/components/PageLoadingCard'
import { PageShell } from '@/pages/components/PageShell'
import { usePageTitle } from '@/pages/hooks/usePageTitle'
import { useI18n } from '@/system/i18n/use-i18n'

import { SubmissionJudgeMessageCard } from './components/SubmissionJudgeMessageCard'
import { SubmissionJudgeResultCard } from './components/SubmissionJudgeResultCard'
import { SubmissionSourceCodeCard } from './components/SubmissionSourceCodeCard'
import { SubmissionSummaryCard } from './components/SubmissionSummaryCard'

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
    <PageShell title={t('submission.detail.heading')} mainClassName="bg-[linear-gradient(180deg,#f8fafc_0%,#edf4fb_100%)]">
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
        <PageLoadingCard message={t('submission.detail.loading')} />
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
    </PageShell>
  )
}
