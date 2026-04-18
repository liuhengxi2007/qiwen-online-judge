import { Link, Navigate, useParams } from 'react-router-dom'
import { Files } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import {
  parseSubmissionId,
  submissionIdValue,
  submissionLanguageLabel,
  submissionVerdictLabel,
  submissionSourceCodeValue,
} from '@/features/submission/domain/submission'
import { formatProblemTitleDisplay, problemSlugValue, useProblemTitleDisplayMode } from '@/features/problem/domain/problem'
import { useSubmissionDetailQuery } from '@/features/submission/hooks/use-submission-detail-query'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { SignedInUser } from '@/shared/components/signed-in-user'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/i18n'

function formatOptionalDurationMs(value: number | null): string {
  if (value === null) {
    return '--'
  }

  return `${value} ms`
}

function formatOptionalMemoryKb(value: number | null): string {
  if (value === null) {
    return '--'
  }

  if (value < 1024) {
    return `${value} KB`
  }

  return `${(value / 1024).toFixed(1)} MB`
}

function formatCodeLength(value: number): string {
  return `${value} B`
}

export function SubmissionDetailPage() {
  const { t } = useI18n()
  usePageTitle(t('submission.detail.pageTitle'))
  const problemTitleDisplayMode = useProblemTitleDisplayMode()
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

  const submissionQuery = useSubmissionDetailQuery(submissionIdResult.value)

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
            <SignedInUser user={user} />
          </div>

          <AncestorNavigation />
        </div>

        {submissionQuery.errorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{submissionQuery.errorMessage}</AlertDescription>
          </Alert>
        ) : null}

        {submissionQuery.isLoading ? (
          <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
            <CardContent className="py-10 text-sm text-slate-500">{t('submission.detail.loading')}</CardContent>
          </Card>
        ) : submissionQuery.submission ? (
          <div className="space-y-6">
            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="flex size-12 items-center justify-center rounded-2xl bg-indigo-100 text-indigo-700">
                    <Files className="size-5" />
                  </div>
                  <div>
                    <CardTitle className="text-2xl text-slate-950">
                      Submission {submissionIdValue(submissionQuery.submission.id)}
                    </CardTitle>
                    <CardDescription className="mt-2 text-sm text-slate-500">{t('submission.detail.heading')}</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="grid gap-4 text-sm text-slate-600 sm:grid-cols-2 lg:grid-cols-8">
                <div>
                  <p className="text-slate-500">{t('submission.list.problem')}</p>
                  <p className="mt-1">
                    <Link
                      className="font-medium text-slate-900 hover:underline"
                      to={`/problems/${problemSlugValue(submissionQuery.submission.problemSlug)}`}
                    >
                      {formatProblemTitleDisplay(
                        submissionQuery.submission.problemTitle,
                        submissionQuery.submission.problemSlug,
                        problemTitleDisplayMode,
                      )}
                    </Link>
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('submission.detail.submitter')}</p>
                  <div className="mt-1">
                    <UserProfileLink user={submissionQuery.submission.submitter} />
                  </div>
                </div>
                <div>
                  <p className="text-slate-500">{t('common.languageLabel')}</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {submissionLanguageLabel(submissionQuery.submission.language)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('common.verdict')}</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {submissionVerdictLabel(submissionQuery.submission.verdict)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('common.submittedAt')}</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {new Date(submissionQuery.submission.submittedAt).toLocaleString()}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('submission.list.timeUsed')}</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {formatOptionalDurationMs(submissionQuery.submission.timeUsedMs)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('submission.list.spaceUsed')}</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {formatOptionalMemoryKb(submissionQuery.submission.memoryUsedKb)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('submission.list.codeLength')}</p>
                  <p className="mt-1 font-medium text-slate-900">{formatCodeLength(submissionQuery.submission.codeLength)}</p>
                </div>
              </CardContent>
            </Card>

            {submissionQuery.submission.judgeMessage ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <CardTitle className="text-xl text-slate-950">{t('submission.detail.judgeMessage')}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                    {submissionQuery.submission.judgeMessage}
                  </p>
                </CardContent>
              </Card>
            ) : null}

            <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
              <CardHeader>
                <CardTitle className="text-xl text-slate-950">{t('submission.detail.sourceCode')}</CardTitle>
                <CardDescription>{t('submission.detail.sourceDescription')}</CardDescription>
              </CardHeader>
              <CardContent>
                <pre className="overflow-x-auto rounded-3xl bg-slate-950 p-6 text-sm leading-7 text-slate-100">
                  <code>{submissionSourceCodeValue(submissionQuery.submission.sourceCode)}</code>
                </pre>
              </CardContent>
            </Card>
          </div>
        ) : null}
      </section>
    </main>
  )
}
