import { Link, Navigate, useParams } from 'react-router-dom'
import { useState } from 'react'
import { Files, RotateCcw, Trash2 } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSessionGuard } from '@/features/auth/hooks/use-session-guard'
import { deleteSubmission, rejudgeSubmission } from '@/features/submission/api/submission-client'
import {
  isTerminalSubmissionStatus,
  parseSubmissionId,
  type SubmissionId,
  submissionIdValue,
  submissionJudgeStateLabel,
  submissionLanguageLabel,
  submissionSourceCodeValue,
  submissionVerdictLabel,
} from '@/features/submission/domain/submission'
import {
  formatCodeLength,
  formatOptionalDurationMs,
  formatOptionalMemoryKb,
  formatOptionalScore,
} from '@/features/submission/components/submission-support'
import { formatProblemTitleDisplay, problemSlugValue, useProblemTitleDisplayMode } from '@/features/problem/domain/problem'
import { useSubmissionDetailQuery } from '@/features/submission/hooks/use-submission-detail-query'
import { HttpClientError } from '@/shared/api/http-client'
import { AppSectionBar } from '@/shared/components/app-section-bar'
import { AncestorNavigation } from '@/shared/components/ancestor-navigation'
import { ConfirmActionDialog } from '@/shared/components/confirm-action-dialog'
import { DateTimeText } from '@/shared/components/date-time-text'
import { UserProfileLink } from '@/shared/components/user-profile-link'
import { usePageTitle } from '@/shared/hooks/use-page-title'
import { useI18n } from '@/shared/i18n/use-i18n'

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
  const [actionErrorMessage, setActionErrorMessage] = useState('')
  const [isDeleting, setIsDeleting] = useState(false)
  const [isRejudging, setIsRejudging] = useState(false)
  const [deleted, setDeleted] = useState(false)
  const submissionQuery = useSubmissionDetailQuery(currentSubmissionId)

  if (deleted) {
    return <Navigate replace to="/submissions" />
  }

  if (!submissionQuery.isLoading && !submissionQuery.submission) {
    return <Navigate replace to="/submissions" />
  }

  async function handleRejudge() {
    setActionErrorMessage('')
    setIsRejudging(true)
    try {
      const submission = await rejudgeSubmission(currentSubmissionId)
      submissionQuery.replaceSubmission(submission)
    } catch (error) {
      setActionErrorMessage(
        error instanceof HttpClientError ? error.message : t('submission.detail.rejudgeFailed'),
      )
    } finally {
      setIsRejudging(false)
    }
  }

  async function handleDelete() {
    setActionErrorMessage('')
    setIsDeleting(true)
    try {
      await deleteSubmission(currentSubmissionId)
      setDeleted(true)
    } catch (error) {
      setActionErrorMessage(error instanceof HttpClientError ? error.message : t('submission.detail.deleteFailed'))
    } finally {
      setIsDeleting(false)
    }
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

        {actionErrorMessage ? (
          <Alert variant="destructive" className="mb-6 rounded-2xl border-rose-200 bg-rose-50/95">
            <AlertDescription className="text-rose-700">{actionErrorMessage}</AlertDescription>
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
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
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

                  {submissionQuery.submission.canManage ? (
                    <div className="flex flex-wrap gap-3">
                      <Button
                        type="button"
                        variant="outline"
                        className="rounded-2xl border-slate-300 bg-white"
                        disabled={isRejudging || isDeleting || !isTerminalSubmissionStatus(submissionQuery.submission.status)}
                        onClick={() => {
                          void handleRejudge()
                        }}
                      >
                        <RotateCcw className="mr-2 size-4" />
                        {isRejudging ? t('submission.detail.rejudgingAction') : t('submission.detail.rejudgeAction')}
                      </Button>

                      <ConfirmActionDialog
                        title={t('submission.detail.deleteConfirmTitle')}
                        description={t('submission.detail.deleteConfirmDescription')}
                        confirmLabel={isDeleting ? t('submission.detail.deletingAction') : t('submission.detail.deleteAction')}
                        destructive
                        onConfirm={() => {
                          void handleDelete()
                        }}
                        trigger={
                          <Button
                            type="button"
                            variant="outline"
                            className="rounded-2xl border-rose-300 bg-white text-rose-700 hover:bg-rose-100 hover:text-rose-800"
                            disabled={isDeleting || isRejudging}
                          >
                            <Trash2 className="mr-2 size-4" />
                            {isDeleting ? t('submission.detail.deletingAction') : t('submission.detail.deleteAction')}
                          </Button>
                        }
                      />
                    </div>
                  ) : null}
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
                    {submissionJudgeStateLabel(
                      submissionQuery.submission.status,
                      submissionQuery.submission.verdict,
                    )}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('common.submittedAt')}</p>
                  <DateTimeText className="mt-1 font-medium text-slate-900" value={submissionQuery.submission.submittedAt} />
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
                  <p className="text-slate-500">{t('submission.list.score')}</p>
                  <p className="mt-1 font-medium text-slate-900">
                    {formatOptionalScore(submissionQuery.submission.score)}
                  </p>
                </div>
                <div>
                  <p className="text-slate-500">{t('submission.list.codeLength')}</p>
                  <p className="mt-1 font-medium text-slate-900">{formatCodeLength(submissionQuery.submission.codeLength)}</p>
                </div>
              </CardContent>
            </Card>

            {submissionQuery.submission.judgeResult ? (
              <Card className="border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
                <CardHeader>
                  <CardTitle className="text-xl text-slate-950">{t('submission.detail.judgeResult')}</CardTitle>
                  <CardDescription>
                    {submissionVerdictLabel(submissionQuery.submission.judgeResult.verdict)}{' '}
                    · {formatOptionalScore(submissionQuery.submission.judgeResult.score)}
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  {submissionQuery.submission.judgeResult.subtasks.map((subtask) => (
                    <div key={subtask.name} className="rounded-lg border border-slate-200">
                      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
                        <div>
                          <p className="font-medium text-slate-950">{subtask.name}</p>
                          <p className="text-sm text-slate-500">
                            {submissionVerdictLabel(subtask.verdict)} · {formatOptionalScore(subtask.score)}
                          </p>
                        </div>
                        <div className="text-sm text-slate-500">
                          {formatOptionalDurationMs(subtask.timeUsedMs)} · {formatOptionalMemoryKb(subtask.memoryUsedKb)}
                        </div>
                      </div>
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[720px] text-left text-sm">
                          <thead className="bg-slate-50 text-slate-500">
                            <tr>
                              <th className="px-4 py-2 font-medium">{t('submission.detail.testcases')}</th>
                              <th className="px-4 py-2 font-medium">{t('common.verdict')}</th>
                              <th className="px-4 py-2 font-medium">{t('submission.list.score')}</th>
                              <th className="px-4 py-2 font-medium">{t('submission.list.timeUsed')}</th>
                              <th className="px-4 py-2 font-medium">{t('submission.list.spaceUsed')}</th>
                            </tr>
                          </thead>
                          <tbody>
                            {subtask.testcases.map((testcase) => (
                              <tr key={testcase.name} className="border-t border-slate-100">
                                <td className="px-4 py-2 font-medium text-slate-900">{testcase.name}</td>
                                <td className="px-4 py-2 text-slate-700">
                                  {submissionVerdictLabel(testcase.verdict)}
                                </td>
                                <td className="px-4 py-2 text-slate-700">{formatOptionalScore(testcase.score)}</td>
                                <td className="px-4 py-2 text-slate-700">{formatOptionalDurationMs(testcase.timeUsedMs)}</td>
                                <td className="px-4 py-2 text-slate-700">{formatOptionalMemoryKb(testcase.memoryUsedKb)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  ))}
                </CardContent>
              </Card>
            ) : null}

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
